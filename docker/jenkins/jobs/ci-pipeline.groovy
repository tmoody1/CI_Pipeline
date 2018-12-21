node('master'){
    stage('pull code'){
        git 'https://github.com/scrappy1987/tracker-docker.git'
    }
    stage('build project'){
        sh 'mvn clean package docker:build'
    }
    stage('scan project'){
        sh 'mvn sonar:sonar'
    }
    stage('checkout config repo'){
        dir('CI-pipeline'){
            git 'https://github.com/tmoody1/CI_Pipeline.git'
        }
    }
    stage('build project'){
       dir('CI-pipeline/app'){
            sh 'docker-compose up -d'
        }
    }
    stage('Test deployment was successful'){
        retry(6){
            sleep(10)
            sh 'nc -z 127.0.0.1 9090'
        }

    }/*
    stage('push image'){
        gcloud auth configure-docker
        docker push gcr.io/${PROJECT_ID}/hello-app:v1
    }*/
}
