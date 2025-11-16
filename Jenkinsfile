pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')

        REMOTE_DIR = "/www/wwwroot/CITSNVN/icsQuizUserService"
        JAR_NAME   = "icsQuizUserService-0.1.jar"
        DOCKER_APP = "icsquiz_user_app"
        SPRING_PORT = "3090"
        IMAGE_NAME  = "icsquiz-user-service:latest"
    }

    stages {

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        stage('Build JAR') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Upload JAR to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -o StrictHostKeyChecking=no -i '${SSH_KEY}' target/${JAR_NAME} ${PROD_USER}@${PROD_HOST}:${REMOTE_DIR}/${JAR_NAME}"
                    """
                }
            }
        }

        stage('Build Docker + Deploy on VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c "
                        ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} '
                            cd ${REMOTE_DIR};
                            echo \"--- Building Docker Image ---\";
                            docker build -t ${IMAGE_NAME} .;

                            echo \"--- Stopping Old Container ---\";
                            docker stop ${DOCKER_APP} || true;
                            docker rm ${DOCKER_APP} || true;

                            echo \"--- Starting New Container ---\";
                            docker run -d --name ${DOCKER_APP} -p ${SPRING_PORT}:3090 --restart unless-stopped ${IMAGE_NAME};
                        '
                    "
                    """
                }
            }
        }
    }

    post {
        success { echo "✅ Deployment Successful!" }
        failure { echo "❌ Deployment Failed!" }
    }
}
