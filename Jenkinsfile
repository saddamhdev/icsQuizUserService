pipeline {
    agent any
    stages {
        stage('SSH test') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'DO_SSH_KEY', keyFileVariable: 'SSH_KEY')]) {
                    sh '''
                        echo "Testing..."
                        ssh -o StrictHostKeyChecking=no -i $SSH_KEY root@159.89.172.251 "echo SUCCESS"
                    '''
                }
            }
        }
    }
}
