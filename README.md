# CI_Pipeline
There are readmes in most directories, these are to help with startup commands and should be referenced if you ever want more information on the specifics of the infrastructure
For a local deploy I use docker compose and for a deploy to the google cloud platform I use ansible. This was mainly just to see how both worked but also to demonstrate the difference between the 2.
Compose is a single file for a docker stack. It works with both local deployments and deployments to 

##Usage Local deploy
create the secrets file
from the root of the repo
replace value with the user/password you want to use
echo "user=value" >> docker/jenkins/secrets.txt
echo "password=value" >> docker/jenkins/secrets.txt
cd docker/jenkins
docker build -t jenkins-ci
cd ../
docker-compose up -d
browse to jenkins, log in and run the initalize job
this will create jenkins jobs using jenkins job builder
run the ci-pipeline job

##Docker
This is aimed at people who have limited exposure to docker, it aims to explain the concept behind some of the docker ideas.
If you have no experience of docker I reccomend https://docs.docker.com/get-started/#recap-and-cheat-sheet
###Layers
It is not possible to understand docker without understanding the concept of layers in docker.
A Layer is basically any command in the docker file, a layer is 1 command only, 2 copies mean 2 layers, etc. With run there is a caveat that you can use multiple && to create a very long cmd, however if you add RUN again it is a new layer. I actually usae this feature in the jenkins image
Each Layer is an image that docker can build from (this is useful for debugging, as you can use the imgae id before the cmd that fails to check the container)
Docker utilizes layers to reduce the work that it needs to do when a change is made.
Any Unchanged command is saved in docker's cache and reused when you next build the image.
Each layer inherits from the one above it, so if you change the layer at the top of the docker file all layers must then be rebuilt.

Tip, if you are working on a command, put it at the bottom of the file. docker will then only need to build your changes, you can then move it back to the top when you have finished.

A docker image is comprised of the difference between each layer, for this reason you should try to keep less layers if possible to make the images smaller (which in turn makes it faster to download/build them)
This means that you will often find poorly built images are larger than they need to be.
Consider the following example
COPY app.tar.gz /apps
RUN tar -xzf /apps/app.tar.gz && \
    rm /apps/app.tar.gz
Docker stores the difference between each line.
So it will add the tar.gz file, store that in the image
then extract the tar and remove the tarball.
But the image will contain both layers, meaning that the image contains the tarball, the extracted tarball and the cmd to remove the tarball.

ADD app.tar.gz /apps/
This is 1 command that will extract the tarball, meaning that the image is smaller (since it now only needs the extracted files)

A common example you will see is any RUN command that installs software should include removing the cache (again check jenkins image)
In the jenkins image I have a run command that installs software and a second one to download a few binaries/setup permissions.
This means that if I need to run a new command I do not need to re-install software.
The last 2 steps are jenkins plugins and jobs, the 2 most likely things to change. Which means for most day to day activities docker only needs to build 1 or 2 layers.
Some people hold best practice is not to include the apt-get update command I have added.
This is because running docker build now does not produce the same result everytime (as new software becomes availiable)
Personally I prefer to include this and accept it as risk that a deploy may fail (since I would argue any organization should test deploys before rolling out to live :) )
It is important to keep the docker containers up to date. There is no point updating servers every 30 days if your app runs on year old docker images

You do not need to explicitly EXPOSE any ports or use VOLUME (the parent jenkins image I am using has both). However if you do then docker compose or docker swarm will auto map these when you start the container (I prefer to specify them so I know exactly where they are)
Note EXPOSE will not map port 8080 to port 8080 on the host but a random port.
Also note that VOLUME changes the way the container works slightly. 
VOLUME /apps
RUN touch /apps/file
will have no effect at all. Because the VOLUME is only mounted at run time. Meaning that any underlying files you have on the disk before mounting a volume will be lost. As a result you should really define volumes after you have finished making any changes to it.


## compose
There is a compose file that is used to stand up the container, this makes it easier to version control the run command.
There is no point versioning your Dockerfiles if to run them takes a different undocumented combination of args per container (eg ports, env vars, volumes, etc)
run with
docker-compose up -d
Drop the -d to get the logs to console or run docker logs "container"
The best documentation for docker is docker's own website, search leaves a lot to be desired but https://docs.docker.com/compose/compose-file/
has all the information you need to set up docker compose files. Some commands reference docker swarm which is where compose files become really important, however docker deliberately made both compose and stack use the same syntax, so the files are usually interchangeable.
To create a compose file include the line at the top that specifies version and then justadd the sections you need, you can tell compose or swarm to build containers 1st using the build section described in their docs, personally I prefer to build as a separate step, this makes deploying fractionally faster but also more consistent. As you must consciously choose to deploy a new version of the image.

By default docker-compose looks for a docker-compose.yml file and will name the project the directory name (jenkins)

##Jenkins
Huge thanks to vitorcaetanoblog on for automating jenkins https://technologyconversations.com/2017/06/16/automating-jenkins-docker-setup/

##Jenkins Job Builder

##App specific components

##Ansible

##kubernetes? 


