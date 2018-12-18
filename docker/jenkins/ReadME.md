# Note this will not run unless you have created your own secrets file
The security.groovy will run based on the env variables in secrets.txt, there are more secure ways of generating this file but as this is a demo I have left it here
Note any user on the jenkins docker container has access to this file, can map a file to jenkins for additional security or pass secrets using swarm secrets or ansible..
The secrets.txt file must contain
user=<user>
password=<password>


# This is where we install and configure jenkins.


The jenkins server is a docker image which can be built with the cmd

docker build -t jenkins-ci .

## compose
There is a compose file that is used to stand up the container, this makes it easier to version control the run command.
There is no point versioning your Dockerfiles if to run them takes a different undocumented combination of args per container (eg ports, env vars, volumes, etc)
run with
docker-compose up -d
Drop the -d to get the logs to console or run docker logs jenkins
The best documentation for docker is docker's own website, search leaves a lot to be desired but https://docs.docker.com/compose/compose-file/
has all the information you need to set up docker compose files.
By default docker-compose looks for a docker-compose.yml file and will name the project the directory name (jenkins)



Huge thanks to vitorcaetanoblog on for automating jenkins https://technologyconversations.com/2017/06/16/automating-jenkins-docker-setup/

