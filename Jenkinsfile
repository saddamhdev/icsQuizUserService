pipeline {
    agent any

    environment {
        APP_NAME     = "icsquiz-user-service"
        IMAGE_NAME   = "icsquiz-user-service:latest"
        REMOTE_DIR   = "/www/wwwroot/CITSNVN/icsQuizUserService"
        DOCKER_APP   = "icsquiz_user_app"
        SPRING_PORT  = "3090"

        JAR_NAME     = "icsQuizUserService-0.1.jar"

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
                sshagent(['DO_SSH_KEY']) {
                    sh """
                        scp -o StrictHostKeyChecking=no target/${JAR_NAME} ${PROD_USER}@${VPS_HOST}:${REMOTE_DIR}/${JAR_NAME}
                    """
                }
            }
        }

        stage('Build Docker Image on VPS') {
            steps {
                sshagent(['DO_SSH_KEY']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${PROD_USER}@${VPS_HOST} '
                            cd ${REMOTE_DIR};
                            docker build -t ${IMAGE_NAME} .;
                        '
                    """
                }
            }
        }

        stage('Deploy Docker App on VPS') {
            steps {
                sshagent(['DO_SSH_KEY']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${PROD_USER}@${VPS_HOST} '
                            cd ${REMOTE_DIR};

                            docker stop ${DOCKER_APP} || true;
                            docker rm ${DOCKER_APP} || true;

                            docker run -d \\
                                --name ${DOCKER_APP} \\
                                -p ${SPRING_PORT}:3090 \\
                                --restart unless-stopped \\
                                ${IMAGE_NAME};
                        '
                    """
                }
            }
        }
    }

    post {
        success { echo "✅ ICS Quiz User Service Deployment Successful!" }
        failure { echo "❌ Deployment Failed!" }
    }
}
