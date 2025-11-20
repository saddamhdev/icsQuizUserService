pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')
        DEPLOY_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
        PORT       = '3090'
    }

    stages {

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
                                withCredentials([sshUserPrivateKey(credentialsId: cred.id, keyFileVariable: 'TMP')]) {
                                    echo "🟢 Credential '${cred.id}' exists (SSH Key)"
                                }
                            } else {
                                withCredentials([string(credentialsId: cred.id, variable: 'TMP')]) {
                                    echo "🟢 Credential '${cred.id}' exists"
                                }
                            }
                        } catch (e) {
                            error("❌ Credential '${cred.id}' NOT FOUND")
                        }
                    }
                }
            }
        }

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        stage('Build') {
            steps {
                bat '''mvn clean package -DskipTests'''
                bat '''echo ===== Showing JAR files ====='''
                bat '''dir target'''
            }
        }

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

        /* ------------------------------------------------------------------
           DEPLOY USING TEMP SSH KEY (WINDOWS FRIENDLY)
        --------------------------------------------------------------------- */
       stage('Deploy JAR to Server') {
           steps {
               withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                   script {

                       // Expand JAR_NAME NOW (Groovy)
                       def jar = JAR_NAME

                       bat """
       echo Creating temporary SSH key...
       set TEMP_KEY=%WORKSPACE%\\id_rsa_temp
       copy \"%SSH_KEY%\" \"%TEMP_KEY%\" >nul

       echo 📤 Uploading JAR...

       \"C:/Program Files/Git/bin/bash.exe\" -c \"
           scp -o StrictHostKeyChecking=no -i '%TEMP_KEY%' target/${jar} ${PROD_USER}@${PROD_HOST}:${DEPLOY_DIR}/${jar}
       \"
       """
                   }
               }
           }
       }


        /* ------------------------------------------------------------------
           START SPRING BOOT REMOTELY
        --------------------------------------------------------------------- */
        stage('Start Spring Boot App (Remote)') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    script {
                        bat '''
echo Creating temporary SSH key...
set TEMP_KEY=%WORKSPACE%\\id_rsa_temp

copy "%SSH_KEY%" "%TEMP_KEY%" >nul

"C:/Program Files/Git/bin/bash.exe" -c "
    ssh -o StrictHostKeyChecking=no -i 'id_rsa_temp' ${PROD_USER}@${PROD_HOST} '
        cd ${DEPLOY_DIR};

        OLD_PID=$(pgrep -f '${JAR_NAME}')
        if [ ! -z "$OLD_PID" ]; then
            kill -9 $OLD_PID
        fi

        nohup java -jar '${JAR_NAME}' --server.port=${PORT} > app.log 2>&1 &
        echo App started.
    '
"
'''
                    }
                }
            }
        }
    }

    post {
        success { echo "✅ Deployment Completed Successfully!" }
        failure { echo "❌ Deployment Failed!" }
    }
}
