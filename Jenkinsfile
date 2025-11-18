pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')

        REMOTE_DIR = "/www/wwwroot/CITSNVN/icsQuizUserService"
        JAR_NAME   = "icsQuizUserService-0.1.jar"
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

        stage('Prepare Remote Directory') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'mkdir -p %REMOTE_DIR%'"
                    '''
                }
            }
        }

        stage('Upload JAR to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -o StrictHostKeyChecking=no -i '%SSH_KEY%' target/%JAR_NAME% %PROD_USER%@%PROD_HOST%:%REMOTE_DIR%/%JAR_NAME%"
                    '''
                }
            }
        }

        stage('Upload Dockerfile to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -o StrictHostKeyChecking=no -i '%SSH_KEY%' Dockerfile %PROD_USER%@%PROD_HOST%:%REMOTE_DIR%/Dockerfile"
                    '''
                }
            }
        }

        stage('Deploy on VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'cd %REMOTE_DIR% && echo === Building Docker Image === && docker build -t icsquiz-user-service:latest . && echo === Stopping Old Container === && docker stop icsquiz_user_app || true && docker rm icsquiz_user_app || true && echo === Starting New Container === && docker run -d --name icsquiz_user_app -p 3090:3090 --restart unless-stopped icsquiz-user-service:latest && sleep 2 && docker ps | grep icsquiz_user_app && echo === Deployment Complete ===' "
                    '''
                }
            }
        }
    }

    post {
        success { echo "✅ Deployment Successful!" }
        failure { echo "❌ Deployment Failed!" }
    }
}