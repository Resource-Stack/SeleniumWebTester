package com.rsi.selenium;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.sql.*;

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
	public String testPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName, String fieldType, String readElement, int currentSchedulerId, int currentTestCaseId, int currentTestSequence) throws NoSuchElementException {
		String status = "Failed";
		// TODO Auto-generated method stub

		//driver.get(url);
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

		if(!status.equalsIgnoreCase("SUCCESS")){
			long resultCaseId = updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId);
			takeScreenshot(conn, resultCaseId);
		}
		else {
			long resultCaseId = updateTestCaseWithSuccess(conn, currentTestCaseId, currentSchedulerId);

		}
		
		return status;
		
	}
	//TODO Add Action URL param. to this method. That way upon successfull completiong of the action, we will be able to check if the action performed successfully.
	public String actionPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName, String fieldType, String readElement, String baseURL, int currentSchedulerId, int currentTestCaseId, int currentTestSequence) throws NoSuchElementException {
		String status = "Failed";
		String currentPageUrl = driver.getCurrentUrl();
		if (!currentPageUrl.equalsIgnoreCase(baseURL) && currentTestSequence == 1)
			driver.get(baseURL);

        if (fieldType.equalsIgnoreCase("anchor")) {
			driver.findElement(By.linkText(fieldName)).sendKeys(Keys.ENTER);
			// TODO new method checkStatus will decide whether or not action resulted in success or failure. params should be actionUrl, readElement. one of the to should be populated.
			status = checkStatus(url, readElement);
		}
		else {
			WebElement clickableElement = driver.findElement(By.id(fieldName));
			//logger.debug("CHECKING WHETHER ACTION WORKS OR NOT: " + driver.findElement(By.name(readElement)));
			clickableElement.click();
			//String valueOfElement = driver.getTitle();
			//logger.debug("Page title is: " + driver.getTitle());
			/*if(titleOfResultPage.equals("Successfully logged in")) {
			driver.close();
			// Check the title of the page
			status = "Success";
			logger.debug("Page title is: " + driver.getTitle());
        }*/
		}

		if(!status.equalsIgnoreCase("SUCCESS")){
			long resultCaseId = updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId);
			takeScreenshot(conn, resultCaseId);
		}
		else {
			long resultCaseId = updateTestCaseWithSuccess(conn, currentTestCaseId, currentSchedulerId);

		}
		
		return status;
		
	}

	private String checkStatus(String url, String readElement) {
		// TODO if url is where the page is at or if readELement is true then return success.
		return "failure";
	}

	public String inputPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName, String field_type, String inputValue, String base_url, int currentSchedulerId, int currentTestCaseId, int currentTestSequence) {
		String status = "Failed";
		String currentPageUrl = driver.getCurrentUrl();
		if (!currentPageUrl.equalsIgnoreCase(base_url) && currentTestSequence == 1)
			driver.get(base_url);
		WebElement element = driver.findElement(By.id(fieldName));
		element.sendKeys(inputValue);
		// TODO new method checkStatus will decide whether or not action resulted in success or failure. params should be actionUrl, readElement. one of the to should be populated.
		status = checkStatus(url, fieldName);

		if(!status.equalsIgnoreCase("SUCCESS")){
			long resultCaseId = updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId);
			takeScreenshot(conn, resultCaseId);
		}
		else {
			long resultCaseId = updateTestCaseWithSuccess(conn, currentTestCaseId, currentSchedulerId);

		}

		return status;

	}

	private long updateTestCaseWithSuccess(Connection conn, int currentTestCaseId, int currentSchedulerId) {
		long newResultCaseId = -1;
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO result_cases (rd_id,test_case_id,scheduler_id) VALUES(?,?,?)");
			pstmt.setInt(1, 1);
			pstmt.setInt(2, currentTestCaseId);
			pstmt.setInt(3, currentSchedulerId);
			if (pstmt.execute()) {
				ResultSet rs = pstmt.getGeneratedKeys();

				if (rs.next()) {
					newResultCaseId = rs.getLong(1);
					logger.info("Inserted ID -" + newResultCaseId); // display inserted record
				}
				logger.info("Inserted TestCase Result id [" + currentTestCaseId + " ], with Success");
			}
			else {
				logger.error("Could not inserted the Test Case id in Results [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Could not update the Test Case with Result for id [" + currentTestCaseId + " ] with Success Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return newResultCaseId;

	}

	private String takeScreenshot(Connection conn, long resultCaseId) {
		String fileName = new Long(resultCaseId).toString()  +".png";
		try
		{ TakesScreenshot ts=(TakesScreenshot)driver;

			File source=ts. getScreenshotAs(OutputType. FILE);
			FileUtils. copyFile(source, new File("./Screenshots/"+ fileName));
			logger.info("Screenshot taken");
			// Now save the file name to the database.
			PreparedStatement pstmt = conn.prepareStatement("UPDATE result_cases SET screenshot_file_location = ? WHERE id = ?");
			pstmt.setString(1,fileName);
			pstmt.setLong(2, resultCaseId);

			pstmt.execute();

		} catch (Exception e) {
			logger.error("Exception while taking screenshot " + e.getMessage());
		}
		return fileName;
	}

	private long updateTestCaseWithError(Connection conn, int currentTestCaseId, int currentSchedulerId) {
		long newResultCaseId = -1;
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO result_cases (rd_id,test_case_id,scheduler_id) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, 2);
			pstmt.setInt(2, currentTestCaseId);
			pstmt.setInt(3, currentSchedulerId);
			pstmt.execute();
			ResultSet rs = pstmt.getGeneratedKeys();

			if (rs.next()) {
				newResultCaseId = rs.getLong(1);
				logger.info("Inserted ID -" + newResultCaseId); // display inserted record
			}
			logger.info("Inserted TestCase Result id [" + currentTestCaseId + " ], with Error");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Could not update the Test Case with Result for id [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return newResultCaseId;
	}
}
