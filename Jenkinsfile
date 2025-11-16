pipeline {
    agent any

    environment {
        APP_NAME     = "icsquiz-user-service"
        IMAGE_NAME   = "icsquiz-user-service:latest"
        REMOTE_DIR   = "/www/wwwroot/CITSNVN/icsQuizUserService"
        DOCKER_APP   = "icsquiz_user_app"
        SPRING_PORT  = "3090"

        JAR_NAME     = "icsQuizUserService-0.1.jar"   // ✅ FIXED JAR NAME

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

        stage('Upload JAR to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat """
                        "C:/Program Files/Git/bin/bash.exe" -c \
                        "scp -o StrictHostKeyChecking=no -i '${SSH_KEY}' target/${JAR_NAME} ${PROD_USER}@${VPS_HOST}:${REMOTE_DIR}/${JAR_NAME}"
                    """
                }
            }
        }

        stage('Build Docker Image on VPS') {
            steps {
                echo "Docker build will be done on VPS after SCP."
            }
        }

        stage('Deploy Docker App on VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat """
                        "C:/Program Files/Git/bin/bash.exe" -c \
                        "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${VPS_HOST} '
                            cd ${REMOTE_DIR};

                            echo \"-- Building Docker Image --\";
                            docker build -t ${IMAGE_NAME} .;

                            echo \"-- Stopping Previous Container --\";
                            docker stop ${DOCKER_APP} || true;
                            docker rm ${DOCKER_APP} || true;

                            echo \"-- Starting New Docker Container --\";
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
            echo "✅ ICS Quiz User Service Deployment Successful!"
        }
        failure {
            echo "❌ Deployment Failed!"
        }
    }
}
