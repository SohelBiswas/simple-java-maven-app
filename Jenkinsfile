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
                    # 1. Update packages and install prerequisites
                    apt-get update && apt-get install -y wget gnupg
                    
                    # 2. Add the official Trivy security repository keys safely
                    wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor -o /usr/share/keyrings/trivy.gpg
                    echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | tee /etc/apt/sources.list.d/trivy.list
                    
                    # 3. Update lists and install the verified Trivy package
                    apt-get update && apt-get install -y trivy
                    
                    # 4. Run the scan across the workspace filesystem
                    trivy fs .
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
