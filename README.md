# CI_Pipeline  
There are readmes in most directories, these are to help with startup commands and should be referenced if you ever want more information on the specifics of the infrastructure.  
For a local deploy I use docker compose and for a deploy to the google cloud platform I use ansible. This was mainly just to see how both worked but also to demonstrate the difference between the 2.  
Compose is a single file for a docker stack. It works with both local deployments and deployments to.   
The table of contents came from [here](https://github.com/ekalinin/github-markdown-toc) thanks to 
ekalinin. 
  
# Contents
   * [CI_Pipeline](#ci_pipeline)
   * [Usage Local deploy](#usage-local-deploy)
   * [Docker](#docker)
   * [Layers](#layers)
   * [Lightweight](#lightweight)
   * [The entrypoint](#the-entrypoint)
   * [Restart policy](#restart-policy)
   * [Compose](#compose)
   * [Jenkins](#jenkins)
   * [Jenkins Job Types](#jenkins-job-types)
   * [Jenkins Job Builder](#jenkins-job-builder)
   * [App specific components](#app-specific-components)
   * [Ansible](#ansible)
   * [kubernetes?](#kubernetes)

# Usage Local deploy  
Create the secrets file by running the following from the root of the repo.  
Replace value with the user/password you want to use  
```  
echo "user=value" >> docker/jenkins/secrets.txt  
echo "password=value" >> docker/jenkins/secrets.txt
cp docker/jenkins/secrets.txt roles/infra/files/secrets.txt
cd docker/jenkins  
docker build -t jenkins-ci  
cd ../  
docker-compose up -d  
```  
Browse to jenkins, log in  
Run the ci-pipeline job  
to destroy run
```
docker-compose down -v
```
# Usage Ansible deploy
Set up ssh to localhost
Take the secrets.txt file from the previous example
```
ssh-keygen -t rsa -b 4096
cat ~/.ssh/id_rsa.pub >> .ssh/authorized_keys
chmod 600 !$
cd ansible
ansible-playbook "/home/tmoody/project/CI_Pipeline/ansible/infra.yml" -i ansible/inv/hosts.ini
```
to destroy run
```
ansible-playbook "/home/tmoody/project/CI_Pipeline/ansible/infra.yml" -i ansible/inv/hosts.ini --extra-vars "state=absent"
```

# Usage Ansible GCP deploy
From [here](https://docs.ansible.com/ansible/latest/scenario_guides/guide_gce.html)
```
pip install requests google-auth
```
Create the service account. If you have not already got a google cloud account just sign up. You get $300 free usage and they say they will inform you before they charge you real money.
Create the service account using the [link](https://console.developers.google.com/iam-admin/serviceaccounts)
Click create service account
Give it a name and a description
If you look at the ID it will have an @"account ID".iam.gserviceaccount.com, copy the account ID somewhere safe, we will need it later
Select Compute Engine -> Compute Admin (this is a test instance)
Click continue
Don't add any users to the account but make sure to create a key.
Save that key securely. This is how we will connect to the account

open ansible/app.yml and test.yml
change service account file and gcp_project to your project and you service account key file.
There seems to be an issue with the documentation. According to ansible and GCP the scope should not contain https:// however I found the code did not work with out it.
Also when using the google platform I noticed it often came with the error "resource temporalily unavailiable click here to reload"
If that message comes up just reload, tends to fix itself.
```
- hosts: all
  become: yes
  roles:
    - app
  vars:
    region: 'us-west1'
    gcp_project: "gleaming-store-226116"
    auth_kind: "serviceaccount"
    service_account_file: "/home/tmoody/google/key/key.json"
    scopes:
      - "https://www.googleapis.com/auth/compute"
```
I have included a test.yml file, this creates just an IP address which is a free product. Run test.yml with
```
ansible-playbook -i inv/hosts.ini test.yml
```
This should error out with "Access Not Configured. Compute Engine API has not been used in project 747355212659 before or it is disabled."
Click the hyperlink in the error message and enable the API
Then run test.yml again.
If it works change state: present to state: absent and run again. If it has not worked just keep debugging until it does.Check that you have the followed all the steps above.

A list of scopes is availiable [here](https://developers.google.com/identity/protocols/googlescopes)


# Docker  
This is aimed at people who have limited exposure to docker, it aims to explain the concept behind some of the docker ideas.    
If you have no experience of docker I reccomend https://docs.docker.com/get-started/#recap-and-cheat-sheet    
```
docker system prune -a
```
The above command will clear out every unused resource, very good for development as it can reclaim disk space if the cache is growing too large (I was clearing out about 4GB each day while building this, obviously I then had to rebuild the cache but this gives you an idea what was going on)  
# Layers  
It is not possible to understand docker without understanding the concept of layers in docker.  
A Layer is basically any command in the docker file, a layer is 1 command only, 2 copies mean 2 layers, etc. With run there is a caveat that you can use multiple && to create a very long cmd, however if you add RUN again it is a new layer. I actually use this feature in the jenkins image.


Each Layer is an image that docker can build from (this is useful for debugging, as you can use the imgae id before the cmd that fails to check the container)  
Docker utilizes layers to reduce the work that it needs to do when a change is made.  
Any unchanged command is saved in docker's cache and reused when you next build the image.  
Each layer inherits from the one above it, so if you change the layer at the top of the docker file all layers must then be rebuilt.  
  
If you are working on a command, put it at the bottom of the file.  Docker will then only need to build your changes, you can then move it back to the top when you have finished.  
  
A docker image is comprised of the difference between each layer, for this reason you should try to keep fewer layers if possible to make the images smaller (which in turn makes it faster to download/build them)  
You will often find poorly built images are larger than they need to be.  
Consider the following example  
```  
COPY app.tar.gz /apps  
RUN tar -xzf /apps/app.tar.gz && \  
    rm /apps/app.tar.gz  
```  
Docker stores the difference between each line.  
So it will:
  - Add the tar.gz file
  - Store that in the image  
  - Extract the tar
  - Remove the tarball.  

But the image will contain both layers, meaning that the image contains the tarball, the extracted tarball and the cmd to remove the tarball.  
```  
ADD app.tar.gz /apps/  
```  
This is 1 command that will extract the tarball, meaning that the image is smaller (since it now only needs the extracted files)  
  
A common example is any RUN command that installs software should include removing the cache (again check jenkins image I remove apt/lists)  

Another trick I use in the jenkins image is 1 RUN command that installs software and a second one to download a few binaries/setup permissions.  
This means that if I need to run a new command I do not need to re-install software.  
The last 2 steps are jenkins plugins and jobs, the 2 most likely things to change. Which means for most day to day activities docker only needs to build 1 or 2 layers. 

Some people hold best practice is not to include the apt-get update command I have added.  
This is because running docker build now does not produce the same result everytime (as new software becomes availiable)  
Personally I prefer to include this and accept it as risk that a deploy may fail (since I would argue any organization should test deploys before rolling out to live :) )  
It is important to keep the docker containers up to date. There is no point updating servers every 30 days if your app runs on year old docker images  
  
You do not need to explicitly EXPOSE any ports or use VOLUME (the parent jenkins image I am using has both). However if you do then docker compose or docker swarm will auto map these when you start the container (I prefer to specify them in the file so I know exactly where they are)  
Note EXPOSE will not map port 8080 to port 8080 on the host but a random port.  
Also note that VOLUME changes the way the image builds slightly.   
```  
VOLUME /apps  
RUN touch /apps/file  
```  
This will have no effect at all. Because the VOLUME is only mounted at run time. Meaning that any underlying files you have on the disk before mounting a volume will be lost(same as if it was mounted to a VM). As a result you should really define volumes after you have finished making any changes to it.  
  
Note that docker images can be built from other images, most companies will have a base image that contains things like repositories/dns/etc set up and build from that.  
# Lightweight  
People will often say docker is "lightweight" without really explaining what that means, my 1st impression of docker was "it is like a VM but with less features". The point of docker is speed. With reasonable automation you can build a server in 5-10 minutes, with docker you can build in 2-3 minutes but deploy in seconds. The advantage of this is obvious but the disadvantage is sometimes hidden. A good docker image should only contain the bare minimum needed to run, this means no text editors/networking tools(in fact no network, docker normally runs on host network). Personally I make sure that I have a method of installing debugging tools onto any container, preferably by configuring the repositories but you can just copy relevant .deb/.rpms if you need to.  

The networking point is actually quite significant for some apps. We found a strange issue at the last place I worked where certain containers only ran on prime number slaves (3 and 5). This was eventually tracked down to IP6 having been disabled on those hosts but not others (Base AMI had been updated by a separate team). Docker is designed to be mostly independent of server it runs on but it is not completely separate to it's host (whereas an AMI is but takes 10 times as long to build/deploy)  

To keep your containers light try to run with as few layers as possible (such as chaining run commands together or adding directories not individual files) and make sure that in any run command you finish by removing anything and everything you don't explicitly need.  
  
# The entrypoint  
If the command that is specified as the entrypoint fails the entire docker container will fail. This is important, you want your apps to fail loudly as that is more likely to be picked up by monitoring. Don't run systemd in a docker container unless you have no choice as this means if the service dies the container will report everything is working.  
  
# Restart policy  
Setting a restart policy for docker is good practice as it improves durability of the app stack The restart triggers on the failure of the container. This is why it is important to set the entrypoint of the container to fail if the app does. The docker container would work if you ran the below.  
```   
ENTRYPOINT tail -f /dev/null    
```  
And ran the app as a service, but if the app fails the contianer will remain. The entrypoint should be the parent process of the app/service.    
If you set one make sure to use restart 'on failure' not restart 'always'. Otherwise there is no way of stopping the container if you need to debug something  
In the app's docker compose file I have set one. I have deliberately not set it for anything else because I wanted to keep the files as simple as possible since this is a training exercise  
  
# Compose  
There is a compose file that is used to stand up the container, this makes it easier to version control the run command.  
There is no point versioning your Dockerfiles if to run them takes a different undocumented combination of args per container (eg ports, env vars, volumes, etc)  
run with  
```  
docker-compose up -d  
```  
Drop the '-d' to get the logs to console or run 
```
docker logs "container"  
```
The best documentation for docker is docker's own website, search leaves a lot to be desired but this [site](https://docs.docker.com/compose/compose-file/)
has all the information you need to set up docker compose files. Some commands reference docker swarm which is where compose files become really important, however docker deliberately made both compose and stack use the same syntax, so the files are usually interchangeable. 

To create a compose file include the line at the top that specifies version and then just add the sections you need, you can tell compose or swarm to build containers 1st using the build section described in their docs, personally I prefer to build as a separate step, this makes deploying fractionally faster but also more consistent. As you must consciously choose to deploy a new version of the image.  
  
By default docker-compose looks for a docker-compose.yml file and will name the project the directory name (docker in this case)  
The advantages of a compose file is that it is easy to convert to a stack file for docker swarm.  
There is 1 yaml file that can easily be understood that contains everything the application needs to run.  
Because jenkins and sonarqube are on their own software defined network jenkins can resolve sonarqube by container name, even if in a swarm the container is on 1 of several hosts (and it moves)  
It also makes it much easier to hand over code to 3rd parties.  
I can define a file, provide a single command that is the same for all applications (docker-compose up -d) and know that whoever pulls this code will see the same results as me.  

The main disadvantage is compose does not support templating. You can reference environment variables in consul but you can't have a default set of parameters you override. Two ways round this are to either configure the host to contain certain files the containers reference or speciy environment variables before you deploy. For example
```
source dev.txt
docker-compose up
source prod.txt
docker-compose up
```
A more elegant solution would be to use ansible to deploy your services/containers. Ansible has several docker modules and with it you can have defaults which are overridden based on group,host,etc.   
# Jenkins  
Huge thanks to Vitor Caetano on for automating jenkins https://technologyconversations.com/2017/06/16/automating-jenkins-docker-setup/  
And Shadid https://stackoverflow.com/questions/42896983/triggering-a-job-on-start-up-in-jenkins  
  
For an automation tool jenkins does not support much automation itself.  
If you are looking to manage jenkins in your organization I would reccomend biting the proverbial bullet and just copy all the xml files you need straight into jenkins. 

I really didn't want to do that as it makes it much harder if you ever update jenkins. As I have less requirements (1 job) I was able to almost automate jenkins without copying config files.  
Any files in ```/usr/share/jenkins/ref/init.d``` will be run after jenkins has set up. However jenkins will not be availiable for users until the commands finish.  

I originally tried to call jenkins job builder as part of the script, however the longer the script waited the longer it took for jenkins to be availiable for users (and hence the longer before job builder could work)  

I then took shadid's idea to create a job that can be run to create everything else, but this required manually logging in to execute it. Also if you stop and start the instance these scripts are executed again (and shadid's is not idempotent)  

The final solution is to call a shell script with a sleep in it, this allows the groovy script z10job-config.groovy to exit, jenkins to come up and then and only then will jenkins job builder make the required jobs. This could be improved by curling the url and checking response code.  

Jenkins executes the scripts in init.d in alphabetical order, as some plugins seem to put their own scripts in here (my init.d was not empty) I named all my scripts z(num)Name.groovy. This means my scripts are executed after everyone else's but I can still manage order.

# Jenkins Job Types

Jenkins has 2 main job types. Freestyle and pipeline. Lots of plugins will add their own "types" of jobs but these usually boil down to freestyle + a few predefined parameters. Freestyle is designed to be user friendly and is more GUI focused. To add a step in jenkins click "add step" select from the drop down menu the thing you want, enter the required information and repeat (for example if you choose pull from git it will automatically ask what git repo)

Pipeline is more code based and uses groovy syntax. Not all plugins support pipeline syntax so make sure to check before installing it. There is a "pipeline syntax" hyperlink at the bottom of any job that uses pipeline syntax, I reccomend using this, it makes it much easier to create jobs and will use the availiable plugins that you have on your specific jenkins. To use it click on the job you want to change, click configure (if you can't see this then you may not have permission) scroll to the bottom and it is just underneath the pipeline script section.

The advantage of pipeline syntax is as it is code based if you need to refactor you can use most text editors and find and replace. Also if you need to extend jenkins' behaviour slightly it is easier in code than drop down boxes.

Any shell command you run in your job will run on the node that calls it, any groovy in your pipeline usually runs on the master. Also each shell command is a separate connection. For example
```
sh 'export user=admin'
sh 'echo $user'
```
will output nothing, as the environment variables are lost on each connection.
```
sh """
   export user=admin
   echo $user
   """
```
Will output admin as this is a single ssh connection. """ is jenkins' way of using a multi line shell.
# Jenkins Job Builder  
This is a fantastic addition to jenkins. It allows you to define jenkins jobs as yaml files which can be versioned in git but as they are scripts it is a bit easier if you need to extend. The update job is there so that you do not need to rebuild jenkins everytime you change a job. You can even pull the groovy scripts from branches in the git repo if you are testing (just make sure to reset to master when done).  
Full documentation can be found [here](https://docs.openstack.org/infra/jenkins-job-builder/).  
I find the documentation to be helpul but sometimes a bit spartan. However it is well worth the effort of learning this as it protects your jenkins from manual changes/provides auditing per job.  
  
# App specific components  
The app comes from https://github.com/scrappy1987/tracker-docker  
It uses spotify's maven docker plugin to build the images.  
Developers like this because it makes it easy for them to manage the app with minimal docker commands and it makes it easy to add jar files they build.  
The docker file can be found in ```src/main/docker/Dockerfile```.  
Personally I would reccomend creating a base docker image that the devops team can manage and then the developer's dockerfile becomes  
```  
FROM base  
ADD trainer.jar trainer.jar  
```  
This makes it easier to manage the image since you are not limited by maven's integration with it/can build the image independent of compiling code.  
While the developers still retain control of the image, if they need new software they can choose to add it to their docker file for testing and then request it when they know it works.  
  
I stand up the docker images the app makes using docker compose which is located in apps  
  
If the app fails to connect to mysql it fails and the docker container stops.    
The proper and elegant method to solve this is to tell the developers to modify their docker file in src/main/docker to check if mysql is up before connecting. (or if you have followed best practice you can update the base image.)   
However as in this case we don't control the app I have just told docker to restart on failure until it works.    
  
  
# Ansible  
Ansible is used for remote execution, it can be used for configuration management but it can also provision or even just run ad hoc commands to a list of servers. It runs over ssh so in order to connect/use it all you need is ansible installed on the master server and an ssh key on the slave. Ansible is written in python but to write playbooks you only need yaml.

Ansible has probably got the best documentation of any product I've used to find how to use a role I normally just type ansible ```module``` and click the link. Every module has a table of all availiable arguments, defaults and a required in red if they are needed.
If you don't know the name of the module I would try the linux command that you would use. You will usually find stackoverflow has a link to the actual module.

Ansible does not support using docker compose to create app stacks however the syntax is very similar. I have included both examples here to see how they compare. 

variables in ansible are referenced using jinja 2 templatine (```"{{ var }}"```)

An ansible playbook should take the directory structure below. Ansible will then look for a main.yml file in each directory, and any file main.yml references. Lots of articles will place variables in vars not defaults. This is poor practice as it makes it very hard to reuse the role, vars are much harder to override than defaults in ansible. For full details see [here](https://docs.ansible.com/ansible/latest/user_guide/playbooks_variables.html#variable-precedence-where-should-i-put-a-variable)

```
├── groups
│   ├── all
│   │   └── main.yml
│   └── group1
│       └── main.yml
├── inv
│   └── hosts.ini
├── playbook.yml
└── roles
    ├── role1
    │   ├── defaults
    │   │   └── main.yml
    │   └── tasks
    │       └── main.yml
    └── role2
        ├── defaults
        │   └── main.yml
        └── tasks
            └── main.yml
```
The file "playbook.yml" should then contain a list of which roles should be applied to which host (in this case we have localhost and all but you could group servers)
each role called will then run the tasks/main.yml file.


In this play you will notice I have parameterized the state of all resources, this is to make it possible to tear down the infrastructure by overwriting the state on the command line.

I think I found a bug with ansible where ordering seems to need to change based on whether you are creating or destroying. The network infra needs to exist before the container is started but also needs to be removed after the container is removed. As a result my tasks/main.yml changes the order based on whether we are creating or destroying. This is not normal for ansible, however I could not find any examples of this working. Also at the time of writing the keep_volumes has an open bug that the volumes survive the removal of the container. As a result I have just added it as a separate section to remove after container is removed.


# kubernetes?   
  
  
