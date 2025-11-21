pipeline {
    agent any
    tools {
        maven 'maven4'
    }

    environment {
        PROD_USER = "root"                       // FIXED ✔
        PROD_HOST = "159.89.172.251"            // FIXED ✔
        DEPLOY_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
        GLOBAL_ENV = '/www/wwwroot/CITSNVN/global.env'
        PORT       = '3090'
    }

    stages {

        stage('Verify Credentials') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'DO_SSH_PASSWORD',
                                                 usernameVariable: 'SSH_USER',
                                                 passwordVariable: 'SSH_PASS')]) {
                    echo "🟢 Credentials are OK."
                }
            }
        }

        stage('Debug Vars') {
            steps {
                sh '''
                    echo HOST=$PROD_HOST
                    echo USER=$PROD_USER
                '''
            }
        }

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                sh 'echo ==== BUILT FILES ===='
                sh 'ls -lah target'
            }
        }

        stage('Detect Built JAR') {
            steps {
                script {
                    env.JAR_NAME = sh(
                        script: "ls target/*.jar | head -n 1 | xargs -n 1 basename",
                        returnStdout: true
                    ).trim()

                    echo "🟢 Detected JAR: ${env.JAR_NAME}"
                }
            }
        }

       stage('Upload JAR to VPS') {
           steps {
               withCredentials([usernamePassword(credentialsId: 'DO_SSH_PASSWORD',
                                                usernameVariable: 'SSH_USER',
                                                passwordVariable: 'SSH_PASS')]) {

                   sh """
                       echo "📤 Uploading JAR to server..."

                       sshpass -p "$SSH_PASS" scp -o StrictHostKeyChecking=no \
                           target/${JAR_NAME} ${PROD_USER}@${PROD_HOST}:${DEPLOY_DIR}/${JAR_NAME}
                   """
               }
           }
       }

            stage('Restart App on VPS') {
                steps {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'DO_SSH_PASSWORD',
                            usernameVariable: 'SSH_USER',
                            passwordVariable: 'SSH_PASS'
                        )
                    ]) {

                        sh 'echo "Restarting app on VPS..."'

                        // 1. Kill old process
                        sh '''
                            sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            pkill -f "icsQuizUserService" || echo no-process
                        '''

                        // 2. Fix directory permissions BEFORE starting app
                        sh '''
                            sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            "chmod -R 755 ${DEPLOY_DIR}; \
                            chmod 644 ${DEPLOY_DIR}/*.jar 2>/dev/null || true; \
                            chmod 644 ${DEPLOY_DIR}/*.sh 2>/dev/null || true"
                        '''

                        // 3. Create startup script on VPS
                        sh '''
                            sshpass -p "$SSH_PASS" ssh -T -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} << SCRIPT
echo "📝 Creating startup script..."
cat > /www/wwwroot/CITSNVN/icsQuizUserService/start.sh << 'EOF'
#!/bin/bash
source /www/wwwroot/CITSNVN/global.env
java -jar /www/wwwroot/CITSNVN/icsQuizUserService/${JAR_NAME} --server.port=3090 >> /www/wwwroot/CITSNVN/icsQuizUserService/app.log 2>&1
EOF
chmod 755 /www/wwwroot/CITSNVN/icsQuizUserService/start.sh
touch /www/wwwroot/CITSNVN/icsQuizUserService/app.log 2>/dev/null || true
chmod 644 /www/wwwroot/CITSNVN/icsQuizUserService/app.log
echo "✅ Startup script created with proper permissions"
ls -lah /www/wwwroot/CITSNVN/icsQuizUserService/ | grep -E 'start.sh|app.log|jar'
SCRIPT
                        '''

                        // 4. Start new process using the script
                        sh '''
                            echo "🚀 Starting application..."
                            sshpass -p "$SSH_PASS" ssh -n -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            "nohup bash ${DEPLOY_DIR}/start.sh > /dev/null 2>&1 &"
                            echo "⏳ Waiting for application to start..."
                            sleep 2
                        '''

                        // 5. Confirm running
                        sh '''
                            sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            pgrep -f "icsQuizUserService" && echo started || echo failed
                        '''

                        // 6. Display last lines of log
                        sh '''
                            echo "📋 Application Log (Last 30 lines):"
                            echo "=================================="
                            sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            "tail -30 ${DEPLOY_DIR}/app.log || echo 'Log file not available yet'"
                            echo "=================================="
                        '''
                    }
                }
            }

    }

    post {
        success { echo "✅ Deployment Completed Successfully!" }
        failure { echo "❌ Deployment Failed!" }
    }
}