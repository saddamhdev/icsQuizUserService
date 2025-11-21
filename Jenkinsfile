pipeline {
    agent any
    tools {
        maven 'maven4'
    }

    environment {
        PROD_USER = "root"                       // FIXED ✔
        PROD_HOST = "159.89.172.251"            // FIXED ✔
        DEPLOY_DIR = '/www/wwwroot/CITSNVN/icsQuizUserService'
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
                    JAR_NAME = sh(
                        script: "ls target/*.jar | head -n 1 | xargs -n 1 basename",
                        returnStdout: true
                    ).trim()

                    echo "🟢 Detected JAR: ${JAR_NAME}"
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
                          usernamePassword(credentialsId: 'DO_SSH_PASSWORD',
                                           usernameVariable: 'SSH_USER',
                                           passwordVariable: 'SSH_PASS')
                      ]) {

                          script {
                              def CMD = """
              cd ${DEPLOY_DIR}

              echo Checking old process...
              OLD_PID=\$(pgrep -f ${JAR_NAME})
              if [ -n "\$OLD_PID" ]; then
                  echo Killing old PID: \$OLD_PID
                  kill -9 \$OLD_PID
              else
                  echo No running instance found
              fi

              echo Starting app on port ${PORT}
              nohup java -jar ${JAR_NAME} --server.port=${PORT} > app.log 2>&1 &
              echo Restart complete
              """

                              sh """
                                  echo "Restarting app on VPS..."
                                  sshpass -p "$SSH_PASS" ssh -q -o LogLevel=ERROR -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} '${CMD}'
                              """
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
