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
        // ---- App-specific values: change these when Chapter's code arrives ----
        SONAR_PROJECT_KEY = 'simple-java-app'
        ARTIFACT_PATH     = 'target/*.jar'
        TEST_REPORTS      = 'target/surefire-reports/*.xml'

        // ---- Tool settings ----
        SONAR_HOST_URL    = 'http://host.docker.internal:9000'
        SONAR_TOKEN       = credentials('jenkins-token')
        TRIVY_SEVERITY    = 'HIGH,CRITICAL'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
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
                    if ! command -v trivy >/dev/null 2>&1; then
                        echo "Trivy not found. Installing..."
                        apt-get update -qq
                        apt-get install -y -qq wget gnupg
                        wget -qO- https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --batch --dearmor -o /usr/share/keyrings/trivy.gpg
                        echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | tee /etc/apt/sources.list.d/trivy.list
                        apt-get update -qq
                        apt-get install -y -qq trivy
                    fi

                    # Fails the build if any HIGH or CRITICAL issue is found
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
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed. Check the stage marked in red for details.'
        }
    }
}
