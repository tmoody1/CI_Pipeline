# Note this will not run unless you have created your own secrets file
The security.groovy will run based on the env variables in secrets.txt, there are more secure ways of generating this file but as this is a demo I have left it here
Note any user on the jenkins docker container has access to this file, can map a file to jenkins for additional security or pass secrets using swarm secrets or ansible..
The secrets.txt file must contain
user=<jenkins user>
password=<jenkins pass>


# This is where we install and configure jenkins.


The jenkins server is a docker image which can be built with the cmd

docker build -t jenkins-ci .