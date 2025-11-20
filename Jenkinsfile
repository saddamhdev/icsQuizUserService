pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')
        DEPLOY_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
        PORT       = '3090'
    }

    stages {

        /* ----------------------------------------
           VERIFY REQUIRED CREDENTIALS
        ------------------------------------------ */
        stage('Verify Required Credentials') {
            steps {
                script {
                    def requiredCreds = [
                        [id: 'DO_HOST',    type: 'string'],
                        [id: 'DO_USER',    type: 'string'],
                        [id: 'DO_SSH_KEY', type: 'ssh']
                    ]

                    requiredCreds.each { cred ->
                        try {
                            if (cred.type == 'ssh') {
                                withCredentials([sshUserPrivateKey(credentialsId: cred.id, keyFileVariable: 'X')]) {
                                    echo "🟢 Credential '${cred.id}' exists (SSH Key)"
                                }
                            } else {
                                withCredentials([string(credentialsId: cred.id, variable: 'X')]) {
                                    echo "🟢 Credential '${cred.id}' exists"
                                }
                            }
                        } catch (e) {
                            error("❌ Credential '${cred.id}' NOT FOUND! Add it to Jenkins.")
                        }
                    }
                }
            }
        }

        /* ----------------------------------------
           CLONE REPOSITORY
        ------------------------------------------ */
        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        /* ----------------------------------------
           BUILD STAGE
        ------------------------------------------ */
        stage('Build') {
            steps {
                bat '''mvn clean package -DskipTests'''
                bat '''echo ===== Showing JAR files in target/ ====='''
                bat '''dir target'''
            }
        }

        /* ----------------------------------------
           FIND BUILT JAR NAME
        ------------------------------------------ */
        stage('Find Built JAR') {
            steps {
                script {
                    JAR_NAME = powershell(
                        script: "(Get-ChildItem -Path 'target/*.jar' -File | Select-Object -First 1).Name",
                        returnStdout: true
                    ).trim()

                    echo "🟢 Detected JAR: ${JAR_NAME}"
                }
            }
        }

        /* ----------------------------------------
           DEPLOY USING SSH-AGENT (NO PATH ISSUES)
        ------------------------------------------ */
        stage('Deploy JAR to Server') {
            steps {
                sshagent(['DO_SSH_KEY']) {
                    bat '''
"C:/Program Files/Git/bin/bash.exe" -c "
    echo 📤 Uploading JAR via ssh-agent...
    scp -o StrictHostKeyChecking=no target/'"${JAR_NAME}"' ${PROD_USER}@${PROD_HOST}:${DEPLOY_DIR}/'${JAR_NAME}'
"
'''
                }
            }
        }

        /* ----------------------------------------
           START REMOTE SPRING BOOT APP (CLEAN!)
        ------------------------------------------ */
        stage('Start Spring Boot App (Remote)') {
            steps {
                sshagent(['DO_SSH_KEY']) {
                    bat '''
"C:/Program Files/Git/bin/bash.exe" -c "
    ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} '
        cd ${DEPLOY_DIR};

        echo 🔍 Checking old process...
        OLD_PID=$(pgrep -f '${JAR_NAME}')
        if [ ! -z "$OLD_PID" ]; then
            echo 🔴 Killing old PID: $OLD_PID
            kill -9 $OLD_PID
        fi

        echo 🚀 Starting Spring Boot App...
        nohup java -Xms64m -Xmx128m -jar '${JAR_NAME}' --server.port=${PORT} > app.log 2>&1 &

        echo 🟢 Application Started on port ${PORT}
    '
"
'''
                }
            }
        }
    }

    post {
        success {
            echo "✅ Deployment Completed Successfully!"
        }
        failure {
            echo "❌ Deployment Failed!"
        }
    }
}
