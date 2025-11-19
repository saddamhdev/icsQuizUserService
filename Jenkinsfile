pipeline {
    agent any

    environment {
        PROD_HOST     = credentials('DO_HOST')
        PROD_USER     = credentials('DO_USER')
        PROJECT_DIR   = '/www/wwwroot/CITSNVN/icsQuizUserService'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('🔍 Clone Repository') {
            steps {
                echo "Cloning repository..."
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
                echo "✅ Repository cloned"
            }
        }

        stage('📤 Upload Project to VPS') {
            steps {
                echo "Uploading project to VPS..."
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i $SSH_KEY $PROD_USER@$PROD_HOST 'rm -rf $PROJECT_DIR && mkdir -p $PROJECT_DIR'"

                    "C:/Program Files/Git/bin/bash.exe" -c "tar -czf - --exclude=.git --exclude=target --exclude=node_modules . | ssh -o StrictHostKeyChecking=no -i $SSH_KEY $PROD_USER@$PROD_HOST 'cd $PROJECT_DIR && tar -xzf -'"

                    echo Upload complete
                    '''
                }
            }
        }

        stage('🚀 Build & Deploy on VPS') {
            steps {
                echo "Building and deploying on VPS..."
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i $SSH_KEY $PROD_USER@$PROD_HOST 'cd $PROJECT_DIR && bash vps-deploy.sh'"
                    '''
                }
            }
        }

        stage('✅ Verification') {
            steps {
                echo "Verifying deployment..."
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i $SSH_KEY $PROD_USER@$PROD_HOST 'docker ps | grep icsquiz_user_app'"
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "✅ DEPLOYMENT SUCCESSFUL!"
        }
        failure {
            echo "❌ DEPLOYMENT FAILED!"
        }
    }
}