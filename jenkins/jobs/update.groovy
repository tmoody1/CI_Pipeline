node('master'){
    stage('pull code'){
        git 'https://github.com/tmoody1/CI_Pipeline.git'
    }
    stage('update jobs'){
        dir('jenkins/jobs'){
            sh 'jenkins-jobs update .'
        }
    }
}


