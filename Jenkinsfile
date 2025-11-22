pipeline {
    agent any
    tools {
        maven 'maven4'
    }

    environment {
        PROD_HOST = "159.89.172.251"
        PROD_USER = "root"
        APP_DIR   = "/www/wwwroot/CITSNVN/icsQuizUserService"
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

        stage('Detect JAR') {
            steps {
                script {
                    env.JAR_NAME = sh(
                        script: "ls target/*.jar | head -1 | xargs -n1 basename",
                        returnStdout: true
                    ).trim()
                }
                echo "JAR Detected: ${env.JAR_NAME}"
            }
        }

        stage('Upload JAR to VPS') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'DO_SSH_PASSWORD',
                    usernameVariable: 'SSH_USER',
                    passwordVariable: 'SSH_PASS'
                )]) {
                    sh """
                        echo "📤 Uploading JAR to VPS..."
                        sshpass -p "$SSH_PASS" scp -o StrictHostKeyChecking=no \
                        target/${JAR_NAME} ${PROD_USER}@${PROD_HOST}:${APP_DIR}/${JAR_NAME}
                    """
                }
            }
        }

        stage('Build Docker Image on VPS') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'DO_SSH_PASSWORD',
                    usernameVariable: 'SSH_USER',
                    passwordVariable: 'SSH_PASS'
                )]) {
                    sh """
                        echo "🐳 Building Docker image on VPS..."
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                        "cd ${APP_DIR} && docker build -t icsquiz-user-app:latest ."
                    """
                }
            }
        }

        stage('Apply Kubernetes YAML') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'DO_SSH_PASSWORD',
                    usernameVariable: 'SSH_USER',
                    passwordVariable: 'SSH_PASS'
                )]) {
                    sh """
                        echo "📄 Applying Kubernetes Deployment & Service..."
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                        "kubectl apply -f ${APP_DIR}/icsquiz-user-app.yaml; \
                         kubectl apply -f ${APP_DIR}/hpa.yaml"
                    """
                }
            }
        }

        stage('Restart Deployment in Kubernetes') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'DO_SSH_PASSWORD',
                    usernameVariable: 'SSH_USER',
                    passwordVariable: 'SSH_PASS'
                )]) {
                    sh """
                        echo "🔄 Restarting Kubernetes Deployment..."
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no ${PROD_USER}@${PROD_HOST} \
                        "kubectl rollout restart deployment icsquiz-user-app && \
                         kubectl rollout status deployment icsquiz-user-app"
                    """
                }
            }
        }
    }

    post {
        success { echo "🚀 SUCCESS: App deployed on Kubernetes!" }
        failure { echo "❌ FAILED: Something went wrong." }
    }
}
