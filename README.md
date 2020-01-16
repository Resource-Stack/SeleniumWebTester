# SeleniumWebTester

#to compile
mvn clean install

# to run
mvn exec:java -Dexec.mainClass="com.rsi.selenium.RsitesterMain" -Dmaven.test.failure.ignore=true

# database configuration
src/main/resources/dbconfig.properties
