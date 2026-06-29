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
                    if ! command -v trivy &> /dev/null; then
                        echo "Trivy not found. Installing..."
                        apt-get update && apt-get install -y wget gnupg
                        
                        # Remove old key file if it exists to prevent 'File exists' failures
                        rm -f /usr/share/keyrings/trivy.gpg
                        
                        # Fetch and safely dearmor the key
                        wget -qO- https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --batch --dearmor -o /usr/share/keyrings/trivy.gpg
                        
                        echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | tee /etc/apt/sources.list.d/trivy.list
                        apt-get update && apt-get install -y trivy
                    else
                        echo "Trivy is already installed. Proceeding directly to scan."
                    fi
                    
                    # Run the scan across the workspace filesystem
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

        post {
        success {
            echo 'Pipeline completed successfully! Archiving build artifacts...'
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}
        }
    }
}
