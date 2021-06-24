# SeleniumWebTester
A Database driven Testing tool for Wbe Based Applications

#to compile
mvn clean install

# to run
mvn exec:java -Dexec.mainClass="com.rsi.selenium.Main" -Dmaven.test.failure.ignore=true

# database configuration
src/main/resources/dbconfig.properties

#To run this application as a daemon install Xvfb
sudo apt-get install xvfb

#set display number to :99
Xvfb :99 -ac &
export DISPLAY=:99


