pipeline {
    agent any

    tools {
        maven 'maven3'
    }

    options {
        // We check out the code ourselves in the Checkout stage
        skipDefaultCheckout()
        // Keep only the last 10 builds so Jenkins doesn't fill the disk
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Stop a stuck build after 30 minutes
        timeout(time: 30, unit: 'MINUTES')
        // Don't run two builds of this pipeline at the same time
        disableConcurrentBuilds()
    }

    environment {
        // ---- App-specific: change these when Chapter's code arrives ----
        SONAR_PROJECT_KEY = 'simple-java-app'
        IMAGE_NAME        = 'simple-java-app'
        ARTIFACT_PATH     = 'target/*.jar'
        TEST_REPORTS      = 'target/surefire-reports/*.xml'
        // Text the container must print for the smoke test to pass
        SMOKE_TEST_EXPECT = 'Hello World!'

        // ---- Pipeline policy ----
        TRIVY_SEVERITY    = 'HIGH,CRITICAL'
        SONAR_TOKEN       = credentials('jenkins-token')

        // Platform settings (SONAR_HOST_URL, REGISTRY, REGISTRY_CREDS, AWS_REGION)
        // are deliberately NOT set here. They come from Jenkins itself (casc.yaml + .env),
        // so this exact Jenkinsfile runs on your Mac and on AWS without edits.
    }

    stages {
        stage('Preflight') {
            steps {
                echo 'Checking this Jenkins has the settings and tools the pipeline needs...'
                sh '''
                    missing=0
                    for var in SONAR_HOST_URL REGISTRY; do
                        if [ -z "$(printenv "$var")" ]; then
                            echo "Missing Jenkins setting: $var (set it in .env / casc.yaml)"
                            missing=1
                        fi
                    done
                    for tool in git mvn docker trivy; do
                        if ! command -v "$tool" >/dev/null 2>&1; then
                            echo "Missing tool on this Jenkins: $tool"
                            missing=1
                        fi
                    done
                    [ "$missing" -eq 0 ]
                    echo "Registry:  $REGISTRY"
                    echo "SonarQube: $SONAR_HOST_URL"
                    docker version --format 'Docker client {{.Client.Version}}, server {{.Server.Version}} ({{.Server.Arch}})'
                '''
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // Tag images as <build number>-<short commit>, e.g. 27-348dfb9
                    def shortSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG  = "${env.BUILD_NUMBER}-${shortSha}"
                    env.IMAGE_REPO = "${env.REGISTRY}/${env.IMAGE_NAME}"
                }
                echo "This build will produce ${env.IMAGE_REPO}:${env.IMAGE_TAG}"
            }
        }

        stage('Compile') {
            steps {
                echo 'Compiling the application...'
                sh 'mvn -B clean compile'
            }
        }

        stage('Run Unit Tests') {
            steps {
                echo 'Running unit tests...'
                sh 'mvn -B test'
            }
            post {
                always {
                    // Shows test results and trends on the Jenkins build page
                    junit allowEmptyResults: true, testResults: "${TEST_REPORTS}"
                }
            }
        }

        stage('Trivy Security Scan') {
            steps {
                echo 'Scanning dependencies and secrets with Trivy...'
                sh '''
                    trivy fs --no-progress \
                        --scanners vuln,secret \
                        --severity "$TRIVY_SEVERITY" \
                        --exit-code 1 \
                        .
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Sending metrics to SonarQube and waiting for the quality gate...'
                sh '''
                    mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                        -Dsonar.token="$SONAR_TOKEN" \
                        -Dsonar.host.url="$SONAR_HOST_URL" \
                        -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
                        -Dsonar.qualitygate.wait=true \
                        -Dsonar.qualitygate.timeout=300
                '''
            }
        }

        stage('Package') {
            steps {
                echo 'Building final JAR file...'
                sh 'mvn -B package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: "${ARTIFACT_PATH}", fingerprint: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "Building image ${IMAGE_REPO}:${IMAGE_TAG}..."
                sh '''
                    docker build --pull \
                        -t "$IMAGE_REPO:$IMAGE_TAG" \
                        -t "$IMAGE_REPO:latest" \
                        .
                '''
            }
        }

        stage('Trivy Image Scan') {
            steps {
                echo 'Scanning the Docker image (OS packages + Java runtime) with Trivy...'
                sh '''
                    # --ignore-unfixed: only fail on problems that have a fix available
                    trivy image --no-progress \
                        --severity "$TRIVY_SEVERITY" \
                        --ignore-unfixed \
                        --exit-code 1 \
                        "$IMAGE_REPO:$IMAGE_TAG"
                '''
            }
        }

        stage('Smoke Test Image') {
            steps {
                echo 'Running the container once to check it starts correctly...'
                sh '''
                    OUTPUT=$(docker run --rm "$IMAGE_REPO:$IMAGE_TAG")
                    echo "Container output: $OUTPUT"
                    echo "$OUTPUT" | grep -qF "$SMOKE_TEST_EXPECT"
                '''
            }
        }

        stage('Push Image') {
            steps {
                echo "Pushing ${IMAGE_REPO}:${IMAGE_TAG} and :latest..."
                script {
                    def push = {
                        sh '''
                            sh ci/registry-login.sh login
                            trap 'sh ci/registry-login.sh logout' EXIT
                            docker push "$IMAGE_REPO:$IMAGE_TAG"
                            docker push "$IMAGE_REPO:latest"
                        '''
                    }
                    if (env.REGISTRY_CREDS?.trim()) {
                        // Docker Hub (or any registry with a username + token)
                        withCredentials([usernamePassword(credentialsId: env.REGISTRY_CREDS,
                                                          usernameVariable: 'REG_USER',
                                                          passwordVariable: 'REG_PASS')]) {
                            push()
                        }
                    } else {
                        // Amazon ECR: no stored credential, the server's IAM role is used
                        push()
                    }
                }
            }
        }
    }

    post {
        always {
            // Remove this build's images from the machine so they don't pile up
            sh 'docker image rm "$IMAGE_REPO:$IMAGE_TAG" "$IMAGE_REPO:latest" >/dev/null 2>&1 || true'
        }
        success {
            echo "Pipeline completed successfully. Pushed ${env.IMAGE_REPO}:${env.IMAGE_TAG}"
        }
        failure {
            echo 'Pipeline failed. Check the stage marked in red for details.'
        }
    }
}
