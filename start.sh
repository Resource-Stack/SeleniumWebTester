#!/bin/bash
echo "Args passed is $#"
if test "$#" -ne 2; then
     echo "illegal number of parameters"
    exit;
fi
cd /home/newprod/SeleniumWebTester
nohup mvn exec:java -Dexec.mainClass="com.rsi.selenium.Main" -Dmaven.test.failure.ignore=true -Dexec.args="$1 $2" &