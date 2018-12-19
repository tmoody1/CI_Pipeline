FROM jenkins/jenkins:lts
MAINTAINER Thomas Moody

# Install software
USER root
COPY secrets.txt /tmp
RUN apt-get update -y && \
    apt-get upgrade -y && \
    apt-get install -y maven python-pip && \
    rm -rf /var/lib/apt/lists/* && \
    pip install jenkins-job-builder && \
    chown jenkins: /tmp/secrets.txt && \
    chmod 400  /tmp/secrets.txt && \
    curl -o /tmp/docker.tar.gz https://download.docker.com/linux/static/stable/x86_64/docker-18.09.0.tgz && \
    tar -C /tmp -xzf /tmp/docker.tar.gz && \
    mv /tmp/docker/* /usr/bin && \
    groupadd docker && \
    usermod -aG docker jenkins && \
    echo ran

## Need compose binary for jenkins jobs, ideally we would pull from git but I couldn't find it
COPY /usr/bin/docker-compose /usr/bin/docker-compose

USER jenkins
## Install plugins
COPY config/plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt && \
    mkdir ~/.m2
    
COPY --chown=jenkins:jenkins config/settings.xml /var/jenkins_home/.m2/

## Setup admin user and 1st job
COPY --chown=jenkins:jenkins config/init.d/z00security.groovy /usr/share/jenkins/ref/init.groovy.d/
COPY --chown=jenkins:jenkins config/init.d/z10job-config.groovy /usr/share/jenkins/ref/init.groovy.d/

## Setup jobs
RUN mkdir -p ~/.config/jenkins_jobs/
COPY --chown=jenkins:jenkins config/jenkins_jobs.ini /var/jenkins_home/.config/jenkins_jobs/ 
COPY --chown=jenkins:jenkins jobs /var/jenkins_home/job-definitions
