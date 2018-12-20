node('master'){
    stage('checkout config repo'){
        dir('CI-pipeline'){
            git 'https://github.com/tmoody1/CI_Pipeline.git'
        }
    }
    stage('build project'){
       dir('CI-pipeline/app'){
            sh 'docker-compose down -v'
        }
    }
}
