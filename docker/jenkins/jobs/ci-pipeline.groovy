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
    stage('run project'){
        sh 'docker run --name trainer-mysql -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=trainer -e MYSQL_USER=trainer_user -e MYSQL_PASSWORD=trainer_pass -d mysql:5.6'
        sleep(30)
        sh 'docker run -p 9090:8080 --name trainer-app --link trainer-mysql:mysql -d trainer/trainer-tracker'
    }
}
