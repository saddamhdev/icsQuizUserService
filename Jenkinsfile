pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')
        REMOTE_DIR = "/www/wwwroot/CITSNVN/icsQuizUserService"
    }

    stages {

        stage('Clone Repository') {
            steps {
                echo "=== Cloning Repository ==="
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
                echo "✅ Repository cloned successfully"
            }
        }

        stage('Prepare Remote Directory') {
            steps {
                echo "=== Preparing Remote Directory ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Testing SSH Connection ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'echo ✅ SSH OK && whoami'"

                    echo === Cleaning and creating remote directory ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'rm -rf %REMOTE_DIR% && mkdir -p %REMOTE_DIR%'"

                    echo ✅ Remote directory ready
                    '''
                }
            }
        }
        // Add this stage to your Jenkinsfile temporarily to debug

        stage('DEBUG: Check SSH Key') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === SSH Key Path ===
                    echo SSH_KEY=%SSH_KEY%

                    echo === Checking if key file exists ===
                    if exist "%SSH_KEY%" (
                        echo ✅ Key file exists
                        dir "%SSH_KEY%"
                    ) else (
                        echo ❌ Key file does NOT exist
                    )

                    echo === Checking key permissions ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ls -la '%SSH_KEY%'"

                    echo === Checking key content (first 5 lines) ===
                    "C:/Program Files/Git/bin/bash.exe" -c "head -5 '%SSH_KEY%'"
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
                    "C:/Program Files/Git/bin/bash.exe" -c "tar -czf - --exclude=.git --exclude=target --exclude=.gitignore --exclude=.DS_Store --exclude=node_modules . | ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'cd %REMOTE_DIR% && tar -xzf -'"

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
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'echo === Project Contents === && ls -la %REMOTE_DIR% && echo === && echo Dockerfile: && cat %REMOTE_DIR%/Dockerfile | head -5'"
                    '''
                }
            }
        }

        stage('Build & Deploy on VPS') {
            steps {
                echo "=== Starting Build and Deployment on VPS ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Executing build and deploy on VPS ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'cd %REMOTE_DIR% && bash vps-deploy.sh'"
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
        }
        failure {
            echo "❌ DEPLOYMENT FAILED!"
        }
    }
}