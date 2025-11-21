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
                            chmod -R 777 ${DEPLOY_DIR}
                        '''

                        // 3. Create startup script on VPS
                        sh '''
                            sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            "cat > ${DEPLOY_DIR}/start.sh << 'EOF'
                            #!/bin/bash
                            source ${GLOBAL_ENV}
                            java -jar ${DEPLOY_DIR}/${JAR_NAME} --server.port=${PORT} >> ${DEPLOY_DIR}/app.log 2>&1
                            EOF
                            chmod +x ${DEPLOY_DIR}/start.sh"
                        '''

                        // 4. Start new process using the script
                        sh '''
                            sshpass -p "$SSH_PASS" ssh -n -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            "nohup ${DEPLOY_DIR}/start.sh &" < /dev/null
                        '''

                        // 5. Confirm running
                        sh '''
                            sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                            pgrep -f "icsQuizUserService" && echo started || echo failed
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