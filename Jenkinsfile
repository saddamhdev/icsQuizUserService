pipeline {
    agent any

    environment {
        APP_NAME     = "icsquiz-user-service"
        IMAGE_NAME   = "icsquiz-user-service:latest"
        REMOTE_DIR   = "/www/wwwroot/CITSNVN/icsQuizUserService"
        DOCKER_APP   = "icsquiz_user_app"
        SPRING_PORT  = "3090"

        VPS_HOST     = credentials('VPS_HOST')
        PROD_USER    = credentials('DO_USER')
    }

    stages {

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        stage('Build Spring Boot JAR') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image (Jenkins)') {
            steps {
                bat """
                    docker build -t ${IMAGE_NAME} .
                """
            }
        }

        stage('Create TAR from Docker Image') {
            steps {
                bat """
                    docker save -o ${APP_NAME}.tar ${IMAGE_NAME}
                """
            }
        }

        stage('Copy Image TAR to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat """
                        "C:/Program Files/Git/bin/bash.exe" -c \
                        "scp -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${APP_NAME}.tar ${PROD_USER}@${VPS_HOST}:${REMOTE_DIR}/${APP_NAME}.tar"
                    """
                }
            }
        }

        stage('Deploy Docker App on VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat """
                        "C:/Program Files/Git/bin/bash.exe" -c \
                        "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${VPS_HOST} '
                            cd ${REMOTE_DIR};
                            echo "-- Loading Docker Image --";
                            docker load -i ${APP_NAME}.tar;

                            echo "-- Stopping Previous Container --";
                            docker stop ${DOCKER_APP} || true;
                            docker rm ${DOCKER_APP} || true;

                            echo "-- Starting New Docker Container --";
                            docker run -d \\
                                --name ${DOCKER_APP} \\
                                -p ${SPRING_PORT}:3090 \\
                                --restart unless-stopped \\
                                ${IMAGE_NAME}
                        '"
                    """
                }
            }
        }

    }

    post {
        success {
            echo "✅ ICS Quiz User Service Docker Deployment Successful!"
        }
        failure {
            echo "❌ Deployment Failed!"
        }
    }
}
