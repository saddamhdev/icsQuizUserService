pipeline {
    agent any

    environment {
        PROD_HOST     = credentials('DO_HOST')
        PROD_USER     = credentials('DO_USER')
        REMOTE_DIR    = '/www/wwwroot/CITSNVN/icsQuizUserService'
        DOCKER_IMAGE  = 'icsquiz-user-service:latest'
        CONTAINER_NAME = 'icsquiz_user_app'
        PORT          = '3090'
    }

    stages {

        stage('Clone Repository') {
            steps {
                echo "=== Cloning Repository ==="
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
                echo "✅ Repository cloned successfully"
            }
        }

        stage('Build JAR with Maven') {
            steps {
                echo "=== Building JAR with Maven ==="
                bat 'mvn clean install'
                echo "✅ JAR built successfully"
            }
        }

        stage('Prepare Remote Directory') {
            steps {
                echo "=== Preparing Remote Directory ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Testing SSH Connection ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'echo ✅ SSH OK && whoami'"

                    echo === Cleaning and creating remote directory ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'rm -rf ${REMOTE_DIR} && mkdir -p ${REMOTE_DIR}'"

                    echo ✅ Remote directory ready
                    '''
                }
            }
        }

        stage('Upload Entire Project to VPS') {
            steps {
                echo "=== Uploading Entire Project ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Compressing and uploading project ===
                    "C:/Program Files/Git/bin/bash.exe" -c "tar -czf - --exclude=.git --exclude=target --exclude=.gitignore --exclude=.DS_Store --exclude=node_modules . | ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'cd ${REMOTE_DIR} && tar -xzf -'"

                    echo ✅ Project uploaded successfully
                    '''
                }
            }
        }

        stage('Verify Upload') {
            steps {
                echo "=== Verifying Files on VPS ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'echo === Project Contents === && ls -la ${REMOTE_DIR} && echo === Checking Dockerfile === && cat ${REMOTE_DIR}/Dockerfile | head -5'"
                    '''
                }
            }
        }

        stage('Build Docker Image on VPS') {
            steps {
                echo "=== Building Docker Image ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'cd ${REMOTE_DIR} && docker build -t ${DOCKER_IMAGE} .'"
                    '''
                }
            }
        }

        stage('Deploy Docker Container') {
            steps {
                echo "=== Deploying Docker Container ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Stopping old container ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'docker stop ${CONTAINER_NAME} 2>/dev/null || true && docker rm ${CONTAINER_NAME} 2>/dev/null || true'"

                    echo === Starting new container ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'docker run -d --name ${CONTAINER_NAME} -p ${PORT}:${PORT} --restart unless-stopped --memory=512m --cpus=1 ${DOCKER_IMAGE}'"

                    echo === Verifying deployment ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'sleep 3 && docker ps | grep ${CONTAINER_NAME}'"
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                echo "=== Checking Application Logs ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} 'docker logs --tail 30 ${CONTAINER_NAME}'"
                    '''
                }
            }
        }

    }

    post {
        always {
            echo "=========================================="
            echo "Pipeline execution completed"
            echo "=========================================="
        }
        success {
            echo "✅ DEPLOYMENT SUCCESSFUL!"
            echo "Device Management service running on port ${PORT}"
        }
        failure {
            echo "❌ DEPLOYMENT FAILED!"
            echo "Check logs above for details"
        }
    }
}