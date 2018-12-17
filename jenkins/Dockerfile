FROM jenkins/jenkins:lts
MAINTAINER Thomas Moody

RUN apt-get update -y && \
    apu-get upgrade -y && \
    rm -rf /var/lib/apt/lists/*

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt
