#!groovy

File infile = new File("/tmp/secrets.txt")
File outfile = new File("/var/jenkins_home/.config/jenkins_jobs/jenkins_jobs.ini")
outfile.append(infile.getText('UTF-8'))

//Run cmd
def sout = new StringBuilder(), serr = new StringBuilder()
def proc = 'jenkins-jobs update /var/jenkins_home/job-definitions'.execute()
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(1000)
println "out> $sout err> $serr"