#!/usr/bin/python

import subprocess
import sys
import os
import re

def exec_cmd(cmd):
    p = subprocess.Popen(cmd)
    p.communicate()
    return p.wait()

def main(args):
    os.chdir("../")

    plugin="org.apache.maven.plugins:maven-release-plugin:2.3.2"
    mvn = "mvn"
    git = "git"

    prepare = plugin + ":prepare"
    perform = plugin + ":perform"

    cmd = [git, "rev-parse", "--abbrev-ref", "HEAD"]
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    branch = p.stdout.readline().strip()
    code = p.wait()
    if code != 0:
        print "unable to detect git branch"
        sys.exit(1)

    if not re.search("^(release|hotfix)-", branch):
        print "not on release or hotfix branch"
        sys.exit(1)

    cmd = [mvn, prepare, "-DdryRun=true", "-B"]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to run release dryrun"
        sys.exit(1)

    cmd = [mvn, prepare, "-B"]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to release"
        sys.exit(1)


    cmd = [mvn, perform, "-B"]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to perform"
        sys.exit(1)

main(sys.argv)

