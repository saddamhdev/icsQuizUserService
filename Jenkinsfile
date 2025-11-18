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

        stage('Upload Deployment Files to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -o StrictHostKeyChecking=no -i '%SSH_KEY%' Dockerfile %PROD_USER%@%PROD_HOST%:%REMOTE_DIR%/Dockerfile"
                    '''
                    withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                        bat '''
                        "C:/Program Files/Git/bin/bash.exe" -c "scp -o StrictHostKeyChecking=no -i '%SSH_KEY%' deploy.sh %PROD_USER%@%PROD_HOST%:%REMOTE_DIR%/deploy.sh"
                        '''
                    }
                }
            }
        }

        stage('Deploy on VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'chmod +x %REMOTE_DIR%/deploy.sh && %REMOTE_DIR%/deploy.sh'"
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