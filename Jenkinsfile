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
echo "=== DEPLOYMENT DEBUG SCRIPT START ==="
echo "=========================================="

echo "=== 1. Environment Info ==="
echo "Current User: $(whoami)"
echo "Current Directory: $(pwd)"
echo "Shell: $SHELL"

echo ""
echo "=== 2. Verifying remote directory ==="
if [ -d "$REMOTE_DIR" ]; then
    echo "✓ Remote directory exists: $REMOTE_DIR"
else
    echo "✗ Remote directory does NOT exist: $REMOTE_DIR"
    exit 1
fi

echo ""
echo "=== 3. Listing files in remote directory ==="
ls -lh "$REMOTE_DIR"

echo ""
echo "=== 4. Checking file integrity ==="
if [ -f "$REMOTE_DIR/Dockerfile" ]; then
    echo "✓ Dockerfile exists"
    echo "File size: $(du -h "$REMOTE_DIR/Dockerfile" | cut -f1)"
    echo "First 20 lines:"
    head -20 "$REMOTE_DIR/Dockerfile"
else
    echo "✗ Dockerfile NOT FOUND"
    exit 1
fi

if [ -f "$REMOTE_DIR/icsQuizUserService-0.1.jar" ]; then
    echo "✓ JAR file exists"
    echo "File size: $(du -h "$REMOTE_DIR/icsQuizUserService-0.1.jar" | cut -f1)"
else
    echo "✗ JAR file NOT FOUND"
    exit 1
fi

echo ""
echo "=== 5. Checking Docker daemon ==="
docker ps
docker --version

echo ""
echo "=== 6. Changing to remote directory ==="
cd "$REMOTE_DIR"
echo "Current directory: $(pwd)"
echo "Files here:"
ls -lh .

echo ""
echo "=== 7. Building Docker image ==="
echo "Running: docker build -t icsquiz-user-service:latest ."
docker build -t icsquiz-user-service:latest .

echo ""
echo "=== 8. Verifying Docker image ==="
docker images | grep icsquiz-user-service

echo ""
echo "=== 9. Stopping old container ==="
docker stop icsquiz_user_app || echo "No running container to stop"
docker rm icsquiz_user_app || echo "No container to remove"

echo ""
echo "=== 10. Starting new container ==="
docker run -d --name icsquiz_user_app -p 3090:3090 --restart unless-stopped icsquiz-user-service:latest

echo ""
echo "=== 11. Verifying deployment ==="
sleep 2
docker ps | grep icsquiz_user_app

echo ""
echo "=========================================="
echo "=== DEPLOYMENT DEBUG SCRIPT SUCCESS ==="
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