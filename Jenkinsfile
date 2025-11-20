pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')
        DEPLOY_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
        PORT       = '3090'
    }

    stages {

        /* ---------------------------------------------------------------
           CHECK ALL REQUIRED CREDENTIALS
        ----------------------------------------------------------------- */
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

        /* ---------------------------------------------------------------
           GIT CLONE
        ----------------------------------------------------------------- */
        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        /* ---------------------------------------------------------------
           BUILD
        ----------------------------------------------------------------- */
        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
                bat 'echo ==== BUILT FILES ===='
                bat 'dir target'
            }
        }

        /* ---------------------------------------------------------------
           DETECT OUTPUT JAR
        ----------------------------------------------------------------- */
        stage('Detect Built JAR') {
            steps {
                script {
                    JAR_NAME = powershell(
                        script: "(Get-ChildItem -Path 'target/*.jar' -File | Select-Object -First 1).Name",
                        returnStdout: true
                    ).trim()

                    if (!JAR_NAME) {
                        error("❌ No JAR found in /target. Build failed.")
                    }

                    echo "🟢 Detected JAR file: ${JAR_NAME}"
                }
            }
        }

        /* ---------------------------------------------------------------
           DEPLOY TO VPS USING SCP (WINDOWS + GIT-BASH SAFE)
        ----------------------------------------------------------------- */
        stage('Deploy JAR to VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    script {
                        bat """
echo Creating temporary SSH key...
set TEMP_KEY=%WORKSPACE%\\id_rsa_temp

copy "%SSH_KEY%" "%TEMP_KEY%" >nul

echo Convert Windows path to Bash path...
for /f \"delims=\" %%i in ('\"C:/Program Files/Git/usr/bin/cygpath.exe\" \"%TEMP_KEY%\"') do set TEMP_KEY_BASH=%%i

echo Bash key path: %TEMP_KEY_BASH%

echo 📤 Uploading JAR...

\"C:/Program Files/Git/bin/bash.exe\" -c \"
scp -o StrictHostKeyChecking=no -i '%TEMP_KEY_BASH%' target/${JAR_NAME} ${PROD_USER}@${PROD_HOST}:${DEPLOY_DIR}/${JAR_NAME}
\"
                        """
                    }
                }
            }
        }

        /* ---------------------------------------------------------------
           REMOTE START SPRING BOOT SERVICE
        ----------------------------------------------------------------- */
        stage('Restart App on VPS') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    script {
                        bat """
echo Creating temporary SSH key...
set TEMP_KEY=%WORKSPACE%\\id_rsa_temp
copy "%SSH_KEY%" "%TEMP_KEY%" >nul

for /f \"delims=\" %%i in ('\"C:/Program Files/Git/usr/bin/cygpath.exe\" \"%TEMP_KEY%\"') do set TEMP_KEY_BASH=%%i

\"C:/Program Files/Git/bin/bash.exe\" -c \"
ssh -o StrictHostKeyChecking=no -i '%TEMP_KEY_BASH%' ${PROD_USER}@${PROD_HOST} '
    cd ${DEPLOY_DIR};

    echo 🔍 Checking old running process...
    OLD_PID=\\\$(pgrep -f ${JAR_NAME})
    if [ ! -z "\\\$OLD_PID" ]; then
        echo 🔴 Killing old process \\\$OLD_PID
        kill -9 \\\$OLD_PID
    fi

    echo 🚀 Starting new app...
    nohup java -jar ${JAR_NAME} --server.port=${PORT} > app.log 2>&1 &
    echo 🟢 App restarted successfully on port ${PORT}
'
\"
                        """
                    }
                }
            }
        }
    }

    post {
        success { echo "✅ Deployment Successful!" }
        failure { echo "❌ Deployment Failed!" }
    }
}
