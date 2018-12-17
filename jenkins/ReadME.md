This is where we install and configure jenkins.
the jenkins server is a docker image which can be build with the cmd

docker build -t jenkins-ci .

There is a compose file that is used to stand up the container, this makes it easier to version control the run command.
run with
docker-compose up




Huge thanks to vitorcaetanoblog on for automating jenkins https://technologyconversations.com/2017/06/16/automating-jenkins-docker-setup/

