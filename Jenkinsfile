pipeline {
    agent any
    
    tools {
        maven 'maven3' 
    }

    environment {
        // Replace with your token from http://localhost:9000
        SONAR_TOKEN = credentials('jenkins-token')
        SONAR_HOST_URL = 'http://host.docker.internal:9000'
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
                sh 'mvn clean compile'
            }
        }

        stage('Run Unit Tests') {
            steps {
                echo 'Running quick unit tests...'
                sh 'mvn test'
            }
        }

        stage('Trivy Security Scan') {
            steps {
                echo 'Checking and running Trivy scanner natively...'
                sh '''
                    # If trivy command does not exist, install it
                    if ! command -v trivy >/dev/null 2>&1; then
                        echo "Trivy not found. Installing..."
                        apt-get update
                        apt-get install -y wget gnupg
                        wget -qO- https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --batch --dearmor -o /usr/share/keyrings/trivy.gpg
                        echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | tee /etc/apt/sources.list.d/trivy.list
                        apt-get update
                        apt-get install -y trivy
                      fi
                      trivy fs .
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Sending metrics to SonarQube...'
                sh '''mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                -Dsonar.token=$SONAR_TOKEN \
                -Dsonar.host.url=http://host.docker.internal:9000 \
                -Dsonar.projectKey=simple-java-app'''
            }
        }

        stage('Package') {
            steps {
                echo 'Building final JAR file...'
                sh 'mvn package -DskipTests'
            }

        post {
        success {
            echo 'Pipeline completed successfully! Archiving build artifacts...'
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}
        }
    }
