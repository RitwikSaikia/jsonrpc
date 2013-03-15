#!/usr/bin/python

import subprocess
import sys
import os

def main(args):
    os.chdir("../")

    plugin="org.apache.maven.plugins:maven-release-plugin:2.3.2"

    mvn = "mvn"
    update = plugin + ":update-versions"

    cmd = [mvn, update, "-DautoVersionSubmodules=true", "-B"]
    if len(args) > 1:
        if len(args) > 2:
            usage()
            return
        else:
            cmd.append("-DdevelopmentVersion=%s" % args[1])

    subprocess.Popen(cmd).communicate()

def usage():
    print "Usage bump-version.sh [<new-version>]"
    sys.exit()


main(sys.argv)

