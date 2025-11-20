pipeline {
    agent any
    tools {
        maven 'maven4'
    }

    environment {
        PROD_HOST = "159.89.172.251"     // manually set
        PROD_USER = "root"               // manually set
        DEPLOY_DIR = "/www/wwwroot/CITSNVN/icsQuizUserService"
        PORT = "3090"
    }

    stages {
        stage('Test SSH Key') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        echo "Testing SSH with Jenkins key..."
                        ssh -o StrictHostKeyChecking=no -i $SSH_KEY root@159.89.172.251 'echo SUCCESS'
                    """
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
                    JAR_NAME = sh(script: "ls target/*.jar | head -n 1 | xargs -n 1 basename", returnStdout: true).trim()
                    echo "🟢 Detected JAR: ${JAR_NAME}"
                }
            }
        }

        stage('Upload JAR to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        echo 📤 Uploading JAR to ${PROD_HOST}
                        scp -o StrictHostKeyChecking=no -i $SSH_KEY target/${JAR_NAME} ${PROD_USER}@${PROD_HOST}:${DEPLOY_DIR}/${JAR_NAME}
                    """
                }
            }
        }

        stage('Restart App on VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no -i $SSH_KEY ${PROD_USER}@${PROD_HOST} << 'EOF'
                            cd ${DEPLOY_DIR}

                            echo 🔍 Checking old process...
                            OLD_PID=\$(pgrep -f ${JAR_NAME})
                            if [ ! -z "\$OLD_PID" ]; then
                                echo 🔴 Killing old PID: \$OLD_PID
                                kill -9 \$OLD_PID
                            fi

                            echo 🚀 Starting app on port ${PORT}
                            nohup java -jar ${JAR_NAME} --server.port=${PORT} > app.log 2>&1 &

                            echo 🟢 App restarted successfully
                        EOF
                    """
                }
            }
        }
    }

    post {
        success { echo "✅ Deployment Completed Successfully!" }
        failure { echo "❌ Deployment Failed!" }
    }
}
