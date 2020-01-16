package com.rsi.selenium;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class RsiChromeTester {
	final static Logger logger = Logger.getLogger(RsiChromeTester.class);
	private WebDriver driver;
	public WebDriver getDriver() {
		return driver;
	}
	public RsiChromeTester() {
		System.setProperty("webdriver.chrome.driver", "C:\\java_libs\\chromedriver.exe");
		driver = new ChromeDriver();
		
	}
	public String loginToApp(String url_to_test, String userNameField, String passwordField, String btnField, 
			String userName, String password, String successElement) throws NoSuchElementException {
		String status = "Failed";
		logger.debug("PARAMS ARE:" +url_to_test+userNameField+passwordField+btnField+userName+password);
		driver.get(url_to_test);
        // Alternatively the same thing can be done like this
        

        WebElement userNameElement = driver.findElement(By.id(userNameField));
		WebElement passwordElement = driver.findElement(By.id(passwordField));

		userNameElement.sendKeys(userName);
		passwordElement.sendKeys(password);
		
		// find submit button and click
		driver.findElement(By.name(btnField)).click();

		// on the result page get page title and some element
		String titleOfResultPage = driver.getTitle();
		logger.debug("Page title is: " + driver.getTitle());
		if(titleOfResultPage.equals("Successfully logged in")) {
			// Check the title of the page
			status = "Success";
			logger.debug("Page title is: " + driver.getTitle());
        }
        //driver.quit();
		
		return status;
	}
	public String testPageElement(String url, String loginName, String loginPwd, String fieldName, String fieldType, String readElement) throws NoSuchElementException {
		String status = "Failed";
		// TODO Auto-generated method stub
		driver.get(url);
        // Alternatively the same thing can be done like this
        

        WebElement userNameElement = driver.findElement(By.id(fieldName));
		String valueOfElement = driver.getTitle();
		logger.debug("Page title is: " + driver.getTitle());
		/*if(titleOfResultPage.equals("Successfully logged in")) {
			driver.close();
			// Check the title of the page
			status = "Success";
			logger.debug("Page title is: " + driver.getTitle());
        }*/
        
		
		return status;
		
	}
	
	public String actionPageElement(String url, String loginName, String loginPwd, String fieldName, String fieldType, String readElement, String baseURL) throws NoSuchElementException {
		String status = "Failed";
		// TODO Auto-generated method stub
		driver.get(baseURL);
        // Alternatively the same thing can be done like this
        

        WebElement userNameElement = driver.findElement(By.id(fieldName));
		logger.debug("CHECKING WHETHER ACTION WORKS OR NOT: " + driver.findElement(By.name(readElement)));
		driver.findElement(By.name(readElement)).click();
		//String valueOfElement = driver.getTitle();
		//logger.debug("Page title is: " + driver.getTitle());
		/*if(titleOfResultPage.equals("Successfully logged in")) {
			driver.close();
			// Check the title of the page
			status = "Success";
			logger.debug("Page title is: " + driver.getTitle());
        }*/
        
		
		return status;
		
	}

}
