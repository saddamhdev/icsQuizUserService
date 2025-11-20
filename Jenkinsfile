pipeline {
    agent any
    tools {
        maven 'maven4'
    }

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')
        DEPLOY_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
        PORT       = '3090'
    }

    stages {

        stage('Verify Credentials') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                        echo "🟢 All required credentials exist."
                    }
                }
            }
        }

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }
        stage('Check Compiler Versions') {
            steps {
                sh '''
                    echo "===== JAVA RUNTIME ====="
                    java -version

                    echo "===== JAVAC COMPILER ====="
                    javac -version

                    echo "===== ENV PATH ====="
                    echo $PATH
                '''
            }
        }

        stage('Check Java Version in Pipeline') {
            steps {
                sh '''
                    which java
                    java -version
                    echo JAVA_HOME=$JAVA_HOME
                    mvn -version
                '''
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
            stage('Show Jenkins Public Key') {
                steps {
                    withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                        sh '''
                            echo "$SSH_KEY" > jenkins_temp_key
                            chmod 600 jenkins_temp_key
                            echo "===== PUBLIC KEY START ====="
                            ssh-keygen -y -f jenkins_temp_key
                            echo "===== PUBLIC KEY END ====="
                        '''
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
