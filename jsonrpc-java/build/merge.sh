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

    git = "git"

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

    cmd = [git, "checkout", "develop"]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to checkout 'develop'"
        sys.exit(1)

    cmd = [git, "merge", "--no-ff", branch]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to merge '" + branch + "' to 'develop'"
        sys.exit(1)

    cmd = [git, "checkout", "master"]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to checkout 'master'"
        sys.exit(1)

    cmd = [git, "merge", "--no-ff", branch + "~1"]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to merge '" + branch + "~1' to 'master'"
        sys.exit(1)

    cmd = [git, "checkout", branch]
    code = exec_cmd(cmd)
    if code != 0:
        print "unable to checkout '" + branch + "'"
        sys.exit(1)



main(sys.argv)

