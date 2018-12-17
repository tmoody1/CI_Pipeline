FROM jenkins/jenkins:lts
MAINTAINER Thomas Moody

# Install software
USER root
RUN apt-get update -y && \
    apt-get upgrade -y && \
    apt-get install -y maven python-pip && \
    rm -rf /var/lib/apt/lists/* && \
    pip install jenkins-job-builder

USER jenkins
## Install plugins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

## Setup admin user
COPY security.groovy /usr/share/jenkins/ref/init.groovy.d/security.groovy
copy jobs /var/jenkins_home/job-definitions