#!/bin/sh
# Deploys the image this build produced to an environment, then checks it is healthy.
# If the new version doesn't become healthy, the previous version is put back.
#
# Usage: sh ci/deploy.sh staging|production
#
# Today this runs containers on the same Docker that Jenkins uses (your Mac).
# When moving to AWS, this script is the part that changes (e.g. to update an ECS service).
set -eu

env_name="${1:?Usage: sh ci/deploy.sh staging|production}"
case "$env_name" in
  staging)    host_port="${STAGING_PORT:-8081}" ;;
  production) host_port="${PRODUCTION_PORT:-8082}" ;;
  *) echo "Unknown environment: $env_name (use staging or production)" >&2; exit 1 ;;
esac

container="${IMAGE_NAME}-${env_name}"
image="${IMAGE_REPO}:${IMAGE_TAG}"
health_url="http://${DEPLOY_HOST:-host.docker.internal}:${host_port}/health"

run_container() {
  # $1 = image to run, $2 = version label
  docker rm -f "$container" >/dev/null 2>&1 || true
  docker run -d --name "$container" \
    --restart unless-stopped \
    -p "${host_port}:8080" \
    -e APP_ENV="$env_name" \
    -e APP_VERSION="$2" \
    "$1" >/dev/null
}

echo "Deploying $image to $env_name (container '$container', port $host_port)"

# Deploy exactly what is in the registry, the same way a real server would
docker pull --quiet "$image"

# Remember what is running now, so we can roll back if the new version is unhealthy
previous_image=$(docker inspect --format '{{.Config.Image}}' "$container" 2>/dev/null || true)
previous_version=$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$container" 2>/dev/null \
  | sed -n 's/^APP_VERSION=//p' || true)

run_container "$image" "$IMAGE_TAG"

echo "Waiting for $health_url to report version $IMAGE_TAG..."
attempt=0
while [ "$attempt" -lt 30 ]; do
  if body=$(curl -fsS "$health_url" 2>/dev/null) && echo "$body" | grep -qF "\"version\":\"$IMAGE_TAG\""; then
    echo "$env_name is healthy: $body"
    exit 0
  fi
  attempt=$((attempt + 1))
  sleep 2
done

echo "ERROR: $env_name did not become healthy within 60 seconds. Container logs:" >&2
docker logs --tail 50 "$container" >&2 || true

if [ -n "$previous_image" ]; then
  echo "Rolling $env_name back to $previous_image (version ${previous_version:-unknown})" >&2
  run_container "$previous_image" "${previous_version:-unknown}"
else
  echo "No previous version to roll back to; removing the failed container." >&2
  docker rm -f "$container" >/dev/null 2>&1 || true
fi
exit 1
