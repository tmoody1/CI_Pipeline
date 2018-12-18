#!/bin/bash
docker run -d -v jenkins_home:/var/jenkins_home -p 8080:8080 -p 50000:50000 -env JAVA_OPTS=-Djenkins.install.runSetupWizard=false --name jenkins jenkins/jenkins:lts
