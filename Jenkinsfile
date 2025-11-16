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

        // ---------------------------
        // UPLOAD JAR TO VPS
        // ---------------------------
        stage('Upload JAR to VPS') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'DO_SSH_KEY',
                                      keyFileVariable: 'SSH_KEY')
                ]) {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c \
                    "scp -o StrictHostKeyChecking=no -i '${SSH_KEY}' target/${APP_NAME}-0.1.jar \
                    ${PROD_USER}@${VPS_HOST}:${REMOTE_DIR}/${APP_NAME}.jar"
                    """
                }
            }
        }

        // ---------------------------
        // BUILD DOCKER IMAGE ON VPS
        // ---------------------------
        stage('Build Docker Image on VPS') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'DO_SSH_KEY',
                                      keyFileVariable: 'SSH_KEY')
                ]) {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c \
                    "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${VPS_HOST} '
                        cd ${REMOTE_DIR};
                        echo \"-- Building Docker Image on VPS --\";
                        docker build -t ${IMAGE_NAME} .
                    '"
                    """
                }
            }
        }

        // ---------------------------
        // DEPLOY CONTAINER ON VPS
        // ---------------------------
        stage('Deploy Docker App on VPS') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'DO_SSH_KEY',
                                      keyFileVariable: 'SSH_KEY')
                ]) {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c \
                    "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${VPS_HOST} '
                        echo \"-- Stopping Old Container --\";

                        docker stop ${DOCKER_APP} || true;
                        docker rm ${DOCKER_APP} || true;

                        echo \"-- Starting New Container --\";

                        docker run -d \
                          --name ${DOCKER_APP} \
                          -p ${SPRING_PORT}:3090 \
                          --restart unless-stopped \
                          ${IMAGE_NAME};

                        echo \"-- Deployment Completed --\";
                    '"
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ ICS Quiz User Service Deployment Successful (Docker on VPS)!"
        }
        failure {
            echo "❌ Deployment failed!"
        }
    }
}
