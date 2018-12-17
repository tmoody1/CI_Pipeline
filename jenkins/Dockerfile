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
    chmod 400  /tmp/secrets.txt

USER jenkins
## Install plugins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

## Setup admin user
COPY security.groovy /usr/share/jenkins/ref/init.groovy.d/security.groovy
copy jobs /var/jenkins_home/job-definitions

## Setup jobs
RUN mkdir -p ~/.config/jenkins_jobs/
COPY --chown=jenkins:jenkins jenkins_jobs.ini /var/jenkins_home/.config/jenkins_jobs/ 
RUN chmod 640 /var/jenkins_home/.config/jenkins_jobs/jenkins_jobs.ini && \
    cat /tmp/secrets.txt >> /var/jenkins_home/.config/jenkins_jobs/jenkins_jobs.ini
CMD jenkins-jobs update ~/job-definitions
