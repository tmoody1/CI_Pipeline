#!groovy

import jenkins.model.*
import hudson.security.*
import jenkins.security.s2m.AdminWhitelistRule

def instance = Jenkins.getInstance()
File infile = new File("/tmp/secrets.txt")
String user = infile.grep(~/^user.*/)
user = user.tokenize('=')[1].replace("]","")
String pass = infile.grep(~/^password.*/)
pass = pass.tokenize('=')[1].replace("]","")
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount(user, pass)
instance.setSecurityRealm(hudsonRealm)

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
instance.setAuthorizationStrategy(strategy)
instance.save()
// Completely unnecessary but can't bear to lose it
/*
Jenkins.instance.getInjector().getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false)


jenkins = Jenkins.instance;

jobName = "Initalize";

dslProject = new hudson.model.FreeStyleProject(jenkins, jobName);
jenkins.add(dslProject, jobName);
job = jenkins.getItem(jobName)
builders = job.getBuildersList()

hudson.tasks.Shell newShell = new hudson.tasks.Shell("jenkins-jobs update ~/job-definitions")
builders.replace(newShell)
*/