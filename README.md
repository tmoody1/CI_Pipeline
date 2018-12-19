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
browse to jenkins, log in
run the ci-pipeline job

##Docker
This is aimed at people who have limited exposure to docker, it aims to explain the concept behind some of the docker ideas.
If you have no experience of docker I reccomend https://docs.docker.com/get-started/#recap-and-cheat-sheet
docker system prune -a will clear out every unused resource, very good for development as it drastically can reclaim disk space if the cache is growing too large (I was clearing out about 4GB each day while building this, obviously I then had to rebuild the cache but give you an idea what was going on)
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

Note that docker images can be built from other images, most companies will have a base image that contains things like repositories/dns/etc set up and build from that.
###Lightweight
People will often say docker is "lightweight" without really explaining what that means, my 1st impression of docker was "it is like a VM but with less features". The point of docker is speed. With reasonable automation ou can build a server in 5-10 minutes with docker you can build in 2-3 minutes but deploy in seconds. The advantage of this is obvious but the disadvantage is sometimes hidden. A good docker image should only contain the bare minimum needed to run, this means no text editors/networking tools(in fact no network, docker normally runs on host network, try editing /etc/resolve.conf if you don't believe me). Personally I make sure that I have a method of installing debugging tools onto any container, preferably by configuring the repositories but you can just copy relevant .deb/.rpms if you need to.
The networking point is actually quite significant for some apps. We found a strange issue at the last place I worked where certain containers only ran on prime number slaves (3 and 5). This was eventually tracked down to IP6 having been disabled on those hosts but not others (Base AMI had been updated by a separate team). Docker is designed to be mostly independent of server it runs on but it is not completely separate to it's host (whereas an AMI is but takes 10 times as long to build/deploy)
To keep your containers light try to run with as few layers as possible (such as chaining run commands together or adding directories not individual files) and make sure that in any run command you finish by removing anything and everything you don't explicitly need.

###The entrypoint
If the command that is specified as the entrypoint fails the entire docker container will fail. This is important, you want your apps to fail loudly as that is more likely to be picked up by monitoring. Don't run systemd in a docker container unless you have no choice as this means if the service dies the container will report everything is working.

###Restart policy
Setting a restart policy for docker is good practice as it improves durability of the app stack, Note that the restart triggers on the failure of the container. This is why it is important to set the entrypoint of the container to fail if the app does. The docker container would work if you ran the below.  
ENTRYPOINT tail -f /dev/null  
And ran the app as a service, but if the app fails the contianer will remain. The entrypoint should be the parent process of the app/service.  
If you set one make sure to use restart on failure not restart always. Otherwise there is no way of stopping the container if you need to debug something
In the app's docker compose file I have set one. I have deliberately not set it for anything else because I wanted to keep the files as simple as possible since this is a training exercise

##Compose
There is a compose file that is used to stand up the container, this makes it easier to version control the run command.
There is no point versioning your Dockerfiles if to run them takes a different undocumented combination of args per container (eg ports, env vars, volumes, etc)
run with
docker-compose up -d
Drop the -d to get the logs to console or run docker logs "container"
The best documentation for docker is docker's own website, search leaves a lot to be desired but https://docs.docker.com/compose/compose-file/
has all the information you need to set up docker compose files. Some commands reference docker swarm which is where compose files become really important, however docker deliberately made both compose and stack use the same syntax, so the files are usually interchangeable.
To create a compose file include the line at the top that specifies version and then justadd the sections you need, you can tell compose or swarm to build containers 1st using the build section described in their docs, personally I prefer to build as a separate step, this makes deploying fractionally faster but also more consistent. As you must consciously choose to deploy a new version of the image.

By default docker-compose looks for a docker-compose.yml file and will name the project the directory name (docker in this case)
The advantages of a compose file is that it is easy to convert to a stack file for docker swarm.
There is 1 yaml file that can easily be understood that contains everything the application needs to run.
Because jenkins and sonarqube are on their own software defined network jenkins can resolve sonarqube by container name, even if in a swarm the container is on 1 of several hosts (and it moves)
It also makes it much easier to hand over code to 3rd parties.
I can define a file, provide a single command that is the same for all applications (docker-compose up -d) and know that whoever pulls this code will see the same results as me.

##Jenkins
Huge thanks to vitorcaetanoblog on for automating jenkins https://technologyconversations.com/2017/06/16/automating-jenkins-docker-setup/
And shadid https://stackoverflow.com/questions/42896983/triggering-a-job-on-start-up-in-jenkins

For an automation tool jenkins does not support much automation itself.
If you are looking to manage jenkins in your organization I would reccomend biting the proverbial bullet and just copy all the xml files you need straight into jenkins.
I really didn't want to do that as it makes it much harder if you ever update jenkins. As I have less requirements (1 job) I was able to almost automate jenkins without copying config files.
Any files in ~jenkins/config/init.d will be run after jenkins has set up. However jenkins will not be availiable for users until the commands finish.
I originally tried to call jenkins job builder as part of the script, however the longer the script waited the longer it took for jenkins to be availiable for users (and hence the longer before job builder could work)
I then took shadid's idea to create a job that can be run to create everything else, but this required manually logging in to execute it. Also if you stop and start the instance these scripts are executed again (and shadid's is not idempotent)
The final solution is to call a shell script with a sleep in it, this allows the groovy z10job-config.groovy to exit, jenkins to come up and then and only then will jenkins job builder make the required jobs. This could be improved by curling the url and checking response code.

##Jenkins Job Builder
This is a fantastic addition to jenkins. It allows you to define jenkins jobs as yaml files which can be versioned in git. I have used pipeline scripts which can also be versioned and added to git. The update job is there so that you do not need to rebuild jenkins everytime you change a job. You can even pull the groovy scripts from branches in the git repo if you are testing (just make sure to reset to master when done)
Full documentation can be found here https://docs.openstack.org/infra/jenkins-job-builder/
I find the documentation to be helpul but sometimes a bit spartan. However it is well worth the effort of learning this as it protects your jenkins from manual changes/provides auditing per job.

##App specific components
The app comes from https://github.com/scrappy1987/tracker-docker
It uses spotify's maven docker plugin to build the images.
Developers like this because it makes it easy for them to manage the app with minimal docker commands and it makes it easy to add jar files
The docker file can be found in src/main/docker/Dockerfile.
Personally I would reccomend creating a base docker image that the devops team can manage and then the developer's dockerfile becomes
FROM base
ADD trainer.jar trainer.jar
This makes it easier to manage the image since you are not limited by maven's integration with it/can build the image independent of compiling code.

I stand up the docker images the app makes using docker compose which is located in apps

If the app fails to connect to mysql it fails and the docker container stops.  
The proper and elegant method to solve this is to tell the developers to modify their docker file in src/main/docker to check if mysql is up before connecting. (or if you have followed best practice you can update the base image.) 
However as in this case we don't control the app I have just told docker to restart on failure until it works.  


##Ansible

##kubernetes? 


