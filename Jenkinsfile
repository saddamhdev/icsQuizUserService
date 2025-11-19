pipeline {
    agent any

    environment {
        PROD_HOST  = credentials('DO_HOST')   // your VPS IP stored as Jenkins credential
        PROD_USER  = credentials('DO_USER')   // your VPS username (root/ubuntu)
        REMOTE_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
    }

    stages {

        stage('Clone Repository') {
            steps {
                echo "=== Cloning Repository ==="
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
                echo "✅ Code pulled successfully"
            }
        }

      stage('Upload Full Project to VPS') {
          steps {
              echo "=== Uploading Entire Project to VPS ==="

              withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {

                  bat """
                  "C:/Program Files/Git/bin/bash.exe" -c "tar -czf - --exclude=.git --exclude=target --exclude=.idea --exclude=*.iml --exclude=node_modules . | ssh -o StrictHostKeyChecking=no -i $SSH_KEY $PROD_USER@$PROD_HOST 'mkdir -p $REMOTE_DIR && cd $REMOTE_DIR && tar -xzf -'"
                  """

              }
          }
      }


        stage('Verify Remote Files') {
            steps {
                echo "=== Checking Files on VPS ==="

                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {

                    bat """
                    "C:/Program Files/Git/bin/bash.exe" -c "
                        ssh -o StrictHostKeyChecking=no -i $SSH_KEY $PROD_USER@$PROD_HOST \\
                        'echo === FILES IN REMOTE DIR === && ls -la $REMOTE_DIR'
                    "
                    """
                }
            }
        }
    }

    post {
        success {
            echo "🎉 Project successfully uploaded to VPS!"
        }
        failure {
            echo "❌ Upload failed — check the SSH key or remote directory"
        }
    }
}
