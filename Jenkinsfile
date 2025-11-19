pipeline {
    agent any

    environment {
        // Credentials stored in Jenkins
        PROD_HOST     = credentials('DO_HOST')
        PROD_USER     = credentials('DO_USER')
        PROJECT_DIR   = "/www/wwwroot/CITSNVN/icsQuizUserService"
    }

    options {
        // Keep last 10 builds only
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Pipeline times out after 30 minutes
        timeout(time: 30, unit: 'MINUTES')
        // Add timestamps to console output
        timestamps()
    }

    stages {
        stage('🔍 Clone Repository') {
            steps {
                script {
                    echo "=========================================="
                    echo "Step 1: Cloning repository from GitHub"
                    echo "=========================================="
                }
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
                echo "✅ Repository cloned successfully"
            }
        }

        stage('📤 Upload Project to VPS') {
            steps {
                script {
                    echo "=========================================="
                    echo "Step 2: Uploading entire project to VPS"
                    echo "=========================================="
                }
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === 1. Preparing remote directory ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'rm -rf %PROJECT_DIR% && mkdir -p %PROJECT_DIR%'"

                    echo === 2. Uploading project files ===
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -o StrictHostKeyChecking=no -r . %PROD_USER%@%PROD_HOST%:%PROJECT_DIR%/ --exclude=.git --exclude=target --exclude=.gitignore --exclude=.DS_Store"

                    echo ✅ Project uploaded successfully
                    '''
                }
            }
        }

        stage('🚀 Build & Deploy on VPS') {
            steps {
                script {
                    echo "=========================================="
                    echo "Step 3: Building and deploying on VPS"
                    echo "=========================================="
                }
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Executing deployment script on VPS ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'cd %PROJECT_DIR% && bash vps-deploy.sh'"
                    '''
                }
            }
        }

        stage('✅ Verification') {
            steps {
                script {
                    echo "=========================================="
                    echo "Step 4: Verifying deployment"
                    echo "=========================================="
                }
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Checking container status ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'docker ps | grep icsquiz_user_app && echo ✅ Container is running'"

                    echo === Checking service logs ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'docker logs --tail 20 icsquiz_user_app'"
                    '''
                }
            }
        }
    }

    post {
        always {
            // Clean up workspace after build
            cleanWs()
            echo "=========================================="
            echo "Pipeline execution completed"
            echo "=========================================="
        }

        success {
            echo "✅ DEPLOYMENT SUCCESSFUL!"
            echo "🌐 Service is running on port 3090"
        }

        failure {
            echo "❌ DEPLOYMENT FAILED!"
            echo "📋 Check the logs above for details"
        }
    }
}