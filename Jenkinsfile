pipeline {
    agent any

    stages {
        stage('SSH Test with Password') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'DO_SSH_PASSWORD',
                                                 usernameVariable: 'SSH_USER',
                                                 passwordVariable: 'SSH_PASS')]) {
                    sh '''
                        echo "Testing password SSH..."
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no $SSH_USER@159.89.172.251 "echo SUCCESS"
                    '''
                }
            }
        }
    }
}
