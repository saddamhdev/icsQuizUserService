pipeline {
    agent any

    environment {
        VPS_HOST = credentials('DO_HOST')       // e.g., 159.89.172.251
        VPS_USER = credentials('DO_USER')       // e.g., root
        SSH_KEY  = credentials('DO_SSH_KEY')
        APP_DIR  = "/www/wwwroot/CITSNVN/icsQuizUserService"
        JAR_NAME = "icsQuizUserService-0.1.jar"
        DOCKER_IMAGE = "icsquiz_user_service"
        APP_PORT = "3090"
    }

    stages {

        stage('Clone Repo') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        stage('Build JAR') {
            steps {
                bat "mvn clean package -DskipTests"
            }
        }

        stage('Upload JAR to VPS') {
            steps {
                script {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -o StrictHostKeyChecking=no -i ${SSH_KEY} target/${JAR_NAME} ${VPS_USER}@${VPS_HOST}:${APP_DIR}/${JAR_NAME}"
                    """
                }
            }
        }

        stage('Build Docker Image on VPS') {
            steps {
                script {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${VPS_USER}@${VPS_HOST} '
                        cd ${APP_DIR};
                        echo Building Docker Image...
                        docker build -t ${DOCKER_IMAGE}:latest .
                    '"
                    """
                }
            }
        }

        stage('Deploy Docker App on VPS') {
            steps {
                script {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i ${SSH_KEY} ${VPS_USER}@${VPS_HOST} '
                        cd ${APP_DIR};

                        echo Stopping old container...
                        if docker ps -q --filter "name=icsquiz-user-app"; then
                            docker stop icsquiz-user-app || true;
                            docker rm icsquiz-user-app || true;
                        fi

                        echo Removing old images...
                        docker image prune -f;

                        echo Starting new container...
                        docker run -d --name icsquiz-user-app -p ${APP_PORT}:${APP_PORT} ${DOCKER_IMAGE}:latest
                    '"
                    """
                }
            }
        }

    }

    post {
        success {
            echo "✅ User Service deployed successfully on port 3090!"
        }
        failure {
            echo "❌ Deployment failed!"
        }
    }
}
