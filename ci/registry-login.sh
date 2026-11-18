#!/bin/sh
# Logs Docker in to (or out of) the registry named in $REGISTRY.
#   Amazon ECR:          uses the AWS CLI and the machine's IAM role (no stored password)
#   Docker Hub / others: uses REG_USER and REG_PASS from a Jenkins credential
# Usage: sh ci/registry-login.sh login|logout
set -eu

action="${1:-login}"
host="${REGISTRY%%/*}"
case "$host" in
  docker.io|index.docker.io) hub=1 ;;
  *) hub=0 ;;
esac

if [ "$action" = "logout" ]; then
  if [ "$hub" -eq 1 ]; then
    docker logout >/dev/null 2>&1 || true
  else
    docker logout "$host" >/dev/null 2>&1 || true
  fi
  exit 0
fi

case "$host" in
  *.dkr.ecr.*.amazonaws.com)
    echo "Logging in to Amazon ECR ($host) using this machine's IAM role..."
    aws ecr get-login-password --region "${AWS_REGION:?AWS_REGION must be set for ECR}" \
      | docker login --username AWS --password-stdin "$host"
    ;;
  *)
    if [ -z "${REG_USER:-}" ] || [ -z "${REG_PASS:-}" ]; then
      echo "No registry credentials found. Set REGISTRY_CREDS to a username/password credential ID." >&2
      exit 1
    fi
    if [ "$hub" -eq 1 ]; then
      echo "Logging in to Docker Hub as $REG_USER..."
      printf '%s' "$REG_PASS" | docker login -u "$REG_USER" --password-stdin
    else
      echo "Logging in to $host as $REG_USER..."
      printf '%s' "$REG_PASS" | docker login -u "$REG_USER" --password-stdin "$host"
    fi
    ;;
esac
