pipeline {
    agent any

    environment {
        // Change these as needed
        DEPLOY_USER = "root"
        DEPLOY_HOST = "159.89.172.251"
        DEPLOY_DIR  = "/www/wwwroot/CITSNVN/icsQuizUserService"
        JAR_NAME    = "icsQuizUserService-0.1.jar"
        PORT        = "3090"   // Your app port
    }

    stages {

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/saddamhdev/icsQuizUserService'
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Deploy to VPS (Password SSH)') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'DO_SSH_PASSWORD',
                                     usernameVariable: 'SSH_USER',
                                     passwordVariable: 'SSH_PASS')
                ]) {

                    sh '''
                        echo "🔐 Copying JAR to server..."
                        sshpass -p "$SSH_PASS" scp -o StrictHostKeyChecking=no \
                            target/$JAR_NAME $SSH_USER@$DEPLOY_HOST:$DEPLOY_DIR/

                        echo "🛑 Stopping old app if running..."
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no $SSH_USER@$DEPLOY_HOST "
                            pgrep -f $JAR_NAME && kill -9 \$(pgrep -f $JAR_NAME) || echo 'No old process'
                        "

                        echo "🚀 Starting new version..."
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no $SSH_USER@$DEPLOY_HOST "
                            nohup java -jar $DEPLOY_DIR/$JAR_NAME --server.port=$PORT > $DEPLOY_DIR/app.log 2>&1 &
                        "

                        echo "✅ Deployment Completed!"
                    '''
                }
            }
        }
    }
}
