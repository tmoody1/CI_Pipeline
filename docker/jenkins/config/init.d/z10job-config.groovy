#!groovy

// users needs a few seconds to finish
File infile = new File("/tmp/secrets.txt")
File outfile = new File("/var/jenkins_home/.config/jenkins_jobs/jenkins_jobs.ini")
outfile.append(infile.getText('UTF-8'))


def sout = new StringBuilder(), serr = new StringBuilder()
def proc = 'sh /var/jenkins_home/startup.sh'.execute()