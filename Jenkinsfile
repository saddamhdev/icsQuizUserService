pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')
        DEPLOY_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
        PORT       = '3090'
    }

    stages {

        stage('Clone Repository') {
            steps {
                git branch: 'main',
                    credentialsId: 'GITHUB_TOKEN',   // FIXED FOR PRIVATE REPO
                    url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
                bat 'echo ===== Showing JAR files in target/ ====='
                bat 'dir target'
            }
        }

        stage('Find Built JAR') {
            steps {
                script {
                    // Detect JAR dynamically
                    JAR_NAME = bat(
                        script: 'for %i in (target\\*.jar) do @echo %~nxi',
                        returnStdout: true
                    ).trim()

                    echo "🟢 JAR detected: ${JAR_NAME}"
                }
            }
        }

        stage('Deploy JAR to Server') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    script {
                        bat """
                        "C:/Program Files/Git/bin/bash.exe" -c "
                        scp -o StrictHostKeyChecking=no -i '${SSH_KEY}' target/${JAR_NAME} ${PROD_USER}@${PROD_HOST}:${DEPLOY_DIR}/${JAR_NAME}
                        "
                        """
                    }
                }
            }
        }

        stage('Start Spring Boot App (Remote)') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    script {
                        bat """
                        "C:/Program Files/Git/bin/bash.exe" -c "
                        ssh -o StrictHostKeyChecking=no -i '${SSH_KEY}' ${PROD_USER}@${PROD_HOST} '
                            cd ${DEPLOY_DIR};

                            echo 🔍 Checking old process...
                            OLD_PID=\$(pgrep -f ${JAR_NAME})
                            if [ ! -z \"\$OLD_PID\" ]; then
                                echo 🔴 Killing old PID: \$OLD_PID
                                kill -9 \$OLD_PID
                            fi

                            echo 🚀 Starting Spring Boot App...
                            nohup java -Xms64m -Xmx128m -jar ${JAR_NAME} --server.port=${PORT} > app.log 2>&1 &
                            echo 🟢 Application Started on port ${PORT}
                        '
                        "
                        """
                    }
                }
            }
        }

    }

    post {
        failure {
            echo "❌ Deployment failed!"
        }
        success {
            echo "✅ Deployment success!"
        }
    }
}
