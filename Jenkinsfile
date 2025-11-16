pipeline {
    agent any

    environment {
        APP_NAME     = "icsquiz-user-service"
        JAR_NAME     = "icsQuizUserService-0.1.jar"
        REMOTE_DIR   = "/www/wwwroot/CITSNVN/icsQuizUserService"
        DOCKER_APP   = "icsquiz_user_app"
        SPRING_PORT  = "3090"

        VPS_HOST     = credentials('VPS_HOST')
        PROD_USER    = credentials('DO_USER')
        SSH_KEY      = credentials('DO_SSH_KEY')
    }

    stages {

        stage('Clone Repository') {
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

                sshPut remote: [
                    name: "icsquiz-vps",     // 🔥 REQUIRED FIELD
                    host: "${VPS_HOST}",
                    user: "${PROD_USER}",
                    identity: "${SSH_KEY}",
                    allowAnyHosts: true
                ],
                from: "target/${JAR_NAME}",
                into: "${REMOTE_DIR}/${JAR_NAME}"
            }
        }

        stage('Deploy Docker App on VPS') {
            steps {

                sshCommand remote: [
                    name: "icsquiz-vps",     // 🔥 REQUIRED FIELD
                    host: "${VPS_HOST}",
                    user: "${PROD_USER}",
                    identity: "${SSH_KEY}",
                    allowAnyHosts: true
                ], command: """
                    echo '--- Updating App on VPS ---';

                    cd ${REMOTE_DIR};

                    echo '--- Building Docker Image ---';
                    docker build -t ${APP_NAME}:latest .

                    echo '--- Stopping Old Container ---';
                    docker stop ${DOCKER_APP} || true;
                    docker rm ${DOCKER_APP} || true;

                    echo '--- Starting New Container ---';
                    docker run -d \
                        --name ${DOCKER_APP} \
                        -p ${SPRING_PORT}:3090 \
                        --restart unless-stopped \
                        ${APP_NAME}:latest;
                """
            }
        }
    }

    post {
        success { echo "✅ Deployment Successful!" }
        failure { echo "❌ Deployment Failed!" }
    }
}
