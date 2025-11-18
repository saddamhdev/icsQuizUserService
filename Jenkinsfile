pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')
        PROD_USER  = credentials('DO_USER')

        REMOTE_DIR = "/www/wwwroot/CITSNVN/icsQuizUserService"
        JAR_NAME   = "icsQuizUserService-0.1.jar"
    }

    stages {

        stage('Clone Repository') {
            steps {
                echo "=== DEBUG: Cloning Repository ==="
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
                echo "=== Repository cloned successfully ==="
            }
        }

        stage('Build JAR') {
            steps {
                echo "=== DEBUG: Building JAR ==="
                echo "Current workspace: ${WORKSPACE}"
                bat '''
                echo === Listing files before build ===
                dir

                echo === Running Maven build ===
                mvn clean package -DskipTests

                echo === Listing files after build ===
                dir target
                '''
            }
        }

        stage('Verify Local Files') {
            steps {
                echo "=== DEBUG: Verifying Local Files ==="
                bat '''
                echo === Checking Dockerfile existence ===
                if exist Dockerfile (
                    echo Dockerfile FOUND
                    type Dockerfile
                ) else (
                    echo ERROR: Dockerfile NOT FOUND
                    dir
                )

                echo === Checking JAR existence ===
                if exist target\\%JAR_NAME% (
                    echo JAR FOUND: target\\%JAR_NAME%
                    dir target\\%JAR_NAME%
                ) else (
                    echo ERROR: JAR NOT FOUND in target
                    dir target
                )
                '''
            }
        }

        stage('Prepare Remote Directory') {
            steps {
                echo "=== DEBUG: Preparing Remote Directory ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Testing SSH Connection ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'echo SSH Connection OK && whoami && pwd'"

                    echo === Creating remote directory ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'mkdir -p %REMOTE_DIR% && echo Directory created at %REMOTE_DIR%'"
                    '''
                }
            }
        }

        stage('Upload JAR to VPS') {
            steps {
                echo "=== DEBUG: Uploading JAR ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Starting SCP transfer for JAR ===
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -v -o StrictHostKeyChecking=no -i '%SSH_KEY%' target/%JAR_NAME% %PROD_USER%@%PROD_HOST%:%REMOTE_DIR%/%JAR_NAME%"

                    echo === Upload complete ===
                    '''
                }
            }
        }

        stage('Upload Dockerfile to VPS') {
            steps {
                echo "=== DEBUG: Uploading Dockerfile ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Starting SCP transfer for Dockerfile ===
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -v -o StrictHostKeyChecking=no -i '%SSH_KEY%' Dockerfile %PROD_USER%@%PROD_HOST%:%REMOTE_DIR%/Dockerfile"

                    echo === Upload complete ===
                    '''
                }
            }
        }

        stage('Verify Files on VPS') {
            steps {
                echo "=== DEBUG: Verifying Remote Files ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'echo === Directory Contents === && ls -lh %REMOTE_DIR% && echo === File Sizes === && du -sh %REMOTE_DIR%/* && echo === Dockerfile Content === && cat %REMOTE_DIR%/Dockerfile && echo === Docker Version === && docker --version'"
                    '''
                }
            }
        }

        stage('Deploy on VPS') {
            steps {
                echo "=== DEBUG: Creating deploy script ==="
                script {
                    writeFile file: 'remote-deploy.sh', text: '''#!/bin/bash
set -e

REMOTE_DIR="/www/wwwroot/CITSNVN/icsQuizUserService"

echo "=========================================="
echo "=== DOCKER BUILD DIAGNOSTICS ==="
echo "=========================================="

echo ""
echo "=== 1. Current User & Permissions ==="
echo "Current User: $(whoami)"
echo "Current UID: $(id -u)"
echo "Current GID: $(id -g)"
echo "Docker Group: $(getent group docker | cut -d: -f3)"

echo ""
echo "=== 2. Directory Permissions ==="
ls -ld "$REMOTE_DIR"
stat "$REMOTE_DIR" | grep -E "Access:|Uid:|Gid:"

echo ""
echo "=== 3. File Permissions ==="
ls -lh "$REMOTE_DIR"

echo ""
echo "=== 4. Dockerfile Specific Details ==="
if [ -f "$REMOTE_DIR/Dockerfile" ]; then
    echo "Dockerfile exists"
    stat "$REMOTE_DIR/Dockerfile"
    echo ""
    echo "Dockerfile Content:"
    cat "$REMOTE_DIR/Dockerfile"
    echo ""
    echo "Dockerfile is readable: $(test -r "$REMOTE_DIR/Dockerfile" && echo 'YES' || echo 'NO')"
else
    echo "Dockerfile NOT FOUND"
fi

echo ""
echo "=== 5. Checking for Windows line endings ==="
if file "$REMOTE_DIR/Dockerfile" | grep -q CRLF; then
    echo "WARNING: Dockerfile has Windows line endings"
    echo "Converting to Unix line endings..."
    sed -i 's/\r$//' "$REMOTE_DIR/Dockerfile"
fi

echo ""
echo "=== 6. Fix File Permissions ==="
chmod 644 "$REMOTE_DIR/Dockerfile"
chmod 644 "$REMOTE_DIR/icsQuizUserService-0.1.jar"
chmod 755 "$REMOTE_DIR"
echo "Permissions updated"

echo ""
echo "=== 7. Docker Build with Absolute Path ==="
cd "$REMOTE_DIR"
docker build -t icsquiz-user-service:latest .

echo ""
echo "=== 8. Verifying Docker image ==="
docker images | grep icsquiz-user-service

echo ""
echo "=== 9. Stopping old container ==="
docker stop icsquiz_user_app 2>/dev/null || echo "No running container"
docker rm icsquiz_user_app 2>/dev/null || echo "No container to remove"

echo ""
echo "=== 10. Starting new container ==="
docker run -d --name icsquiz_user_app -p 3090:3090 --restart unless-stopped icsquiz-user-service:latest

echo ""
echo "=== 11. Verifying deployment ==="
sleep 2
docker ps | grep icsquiz_user_app

echo ""
echo "=========================================="
echo "=== DEPLOYMENT SUCCESSFUL ==="
echo "=========================================="
'''
                    echo "Deploy script created"
                    bat 'type remote-deploy.sh'
                }

                echo "=== DEBUG: Uploading and executing deploy script ==="
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    bat '''
                    echo === Uploading deploy script ===
                    "C:/Program Files/Git/bin/bash.exe" -c "scp -v -o StrictHostKeyChecking=no -i '%SSH_KEY%' remote-deploy.sh %PROD_USER%@%PROD_HOST%:/tmp/remote-deploy.sh"

                    echo === Executing deploy script ===
                    "C:/Program Files/Git/bin/bash.exe" -c "ssh -o StrictHostKeyChecking=no -i '%SSH_KEY%' %PROD_USER%@%PROD_HOST% 'chmod +x /tmp/remote-deploy.sh && bash -x /tmp/remote-deploy.sh'"
                    '''
                }
            }
        }

    }

    post {
        always {
            echo "=========================================="
            echo "=== PIPELINE EXECUTION COMPLETE ==="
            echo "=========================================="
            bat '''
            echo === Final local workspace state ===
            dir
            echo === JAR exists: ===
            if exist target\\%JAR_NAME% (
                echo YES - target\\%JAR_NAME%
            ) else (
                echo NO - JAR not found
            )
            echo === Dockerfile exists: ===
            if exist Dockerfile (
                echo YES
            ) else (
                echo NO
            )
            '''
        }
        success {
            echo "✅ DEPLOYMENT SUCCESSFUL!"
        }
        failure {
            echo "❌ DEPLOYMENT FAILED! Check logs above for details."
        }
    }
}