pipeline {
    agent any
    
    tools {
        maven 'maven3' 
    }

    environment {
        // Replace with your token from http://localhost:9000
        SONAR_TOKEN = 'your_actual_sonarqube_token_here'
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
                echo 'Downloading and running Trivy scanner...'
                sh '''
                    if [ ! -f ./trivy ]; then
                        curl -sfL https://raw.githubusercontent.com/aquasec/trivy/main/contrib/install.sh | sh -s -- -b . v0.48.3
                    fi
                    ./trivy fs .
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Sending metrics to SonarQube...'
                sh "mvn sonar:sonar -Dsonar.token=${SONAR_TOKEN} -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.projectKey=simple-java-app"
            }
        }

        stage('Package') {
            steps {
                echo 'Building final JAR file...'
                sh 'mvn package -DskipTests'
            }
        }
    }
}
