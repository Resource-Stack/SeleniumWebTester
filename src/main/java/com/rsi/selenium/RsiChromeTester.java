package com.rsi.selenium;

import com.rsi.utils.RsiTestingHelper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RsiChromeTester {
	final static Logger logger = Logger.getLogger(RsiChromeTester.class);
	private WebDriver driver;
	private ChromeOptions options = new ChromeOptions();

	public WebDriver getDriver() {
		return driver;
	}

	public RsiChromeTester(String runType) {
		System.setProperty("webdriver.chrome.driver", "/home/sparsha/Downloads/chromedriver");
		// options.addArguments("--headless");
		// options.addArguments("--start-maximized");
		options.addArguments("--" + runType);
		driver = new ChromeDriver(options);

	}

	public String loginToApp(String url_to_test, String userNameField, String passwordField, String btnField,
			String userName, String password, String successElement) throws NoSuchElementException {
		String status = "Failure";
		logger.debug("PARAMS ARE:" + url_to_test + userNameField + passwordField + btnField + userName + password);
		driver.get(url_to_test);
		// Alternatively the same thing can be done like this

		// TODO Prestep - Prior to login the code needs to browse to the verify step.
		// if (url_to_test.contains("dev11.resourcestack.com")) {
		if (commonHealthCoreUrl(url_to_test)) {
			WebElement emailElement = driver.findElement(
					By.xpath("/html/body/div[1]/div/div/div/div/div/div/div[4]/form/fieldset/div[1]/div/input"));
			emailElement.sendKeys(userName);
			WebElement verifyElement = driver.findElement(
					By.xpath("/html/body/div[1]/div/div/div/div/div/div/div[4]/form/fieldset/div[2]/div/button"));
			verifyElement.click();
			try {
				TimeUnit.SECONDS.sleep(15);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		WebElement userNameElement;
		WebElement passwordElement;
		try {
			userNameElement = driver.findElement(By.id(userNameField));
		} catch (NoSuchElementException nse) {
			userNameElement = driver.findElement(By.name(userNameField));
		}
		try {
			passwordElement = driver.findElement(By.id(passwordField));
		} catch (NoSuchElementException nse) {
			passwordElement = driver.findElement(By.name(passwordField));
		}
		userNameElement.sendKeys(userName);
		passwordElement.sendKeys(password);

		// find submit button and click
		try {
			driver.findElement(By.name(btnField)).click();

		} catch (NoSuchElementException nse) {
			logger.error("btnField [" + btnField
					+ "] with id not found. try and see if hitting Enter works on a submit form");
			passwordElement.sendKeys(Keys.ENTER);

		}

		if (commonHealthCoreUrl(url_to_test)) {
			WebDriverWait wait = new WebDriverWait(driver, 3000);
			wait.until(ExpectedConditions.alertIsPresent());
			Alert alert = driver.switchTo().alert();
			alert.accept();

			// 03212020 Next lines commented since the second alert is no longer valid in
			// the CHC login process.
			// wait.until(ExpectedConditions.alertIsPresent());
			// alert = driver.switchTo().alert();
			// alert.accept();
		}

		// on the result page get page title and some element
		String titleOfResultPage = driver.getTitle();
		logger.debug("Page title is: " + driver.getTitle());
		if (titleOfResultPage.equals("Successfully logged in")) {
			// Check the title of the page
			status = "Success";
			logger.debug("Page title is: " + driver.getTitle());
		}
		// driver.quit();

		return status;
	}

	private boolean commonHealthCoreUrl(String url_to_test) {
		String[] chcUrls = { "dev11.resourcestack.com", "demo.commonhealthcore.org" };
		boolean status = false;

		for (String s : chcUrls) {
			if (url_to_test.contains(s)) {
				status = true;
			}
		}

		return status;
	}

	public String testPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName,
			String xpath, String fieldType, String readElement, String initialDescription, int currentSchedulerId,
			int currentTestCaseId, int currentTestSequence, int resultSuiteId) {
		String status = "Initial";
		WebElement userNameElement = null;
		String description = initialDescription;
		String valueOfElement = null;

		try {
			if ((fieldName == null || fieldName.trim().length() == 0) || (xpath != null && xpath.trim().length() > 0)) {
				if (xpath == null || xpath.trim().length() == 0) {
					if (!com.rsi.utils.RsiTestingHelper.checkEmpty(readElement)) {
						// Now try to identify the first occurrence of fieldType for the readElement.
						if (fieldType.equalsIgnoreCase("h3")) {
							List<WebElement> elements = driver.findElements(By.tagName("h3"));

							// now iterate through the list of elements and see if anyone has the content
							// you are looking for.
							for (WebElement w : elements) {
								if (w.getText().equalsIgnoreCase(readElement)
										|| w.getText().matches(readElement.substring(1, readElement.length() - 1))) {
									status = "Success";
								}
							}
						} else if (fieldType.equalsIgnoreCase("b")) {
							List<WebElement> elements = driver.findElements(By.tagName("b"));

							// now iterate through the list of elements and see if anyone has the content
							// you are looking for.
							for (WebElement w : elements) {
								if (w.getText().equalsIgnoreCase(readElement)
										|| w.getText().matches(readElement.substring(1, readElement.length() - 1))) {
									status = "Success";
								}
							}
						} else if (fieldType.equalsIgnoreCase("label")) {
							List<WebElement> elements = driver.findElements(By.tagName("label"));

							// now iterate through the list of elements and see if anyone has the content
							// you are looking for.
							for (WebElement w : elements) {
								if (w.getText().equalsIgnoreCase(readElement)
										|| w.getText().matches(readElement.substring(1, readElement.length() - 1))) {
									status = "Success";
								}
							}
						}

					} else {
						description = description.concat(" read element was blank. dont know how to compare.");
						status = "Failure";
					}

				} else {
					userNameElement = fetchWebElement("INSPECT", fieldName, xpath);
				}
			} else {
				userNameElement = driver.findElement(By.id(fieldName));
			}

			if (fieldType.equalsIgnoreCase("label") || fieldType.equalsIgnoreCase("text")
					|| fieldType.equalsIgnoreCase("textarea")) {
				valueOfElement = userNameElement.getAttribute("value");
			} else if (fieldType.equalsIgnoreCase("th") || fieldType.equalsIgnoreCase("td")
					|| fieldType.equalsIgnoreCase("li") || fieldType.equalsIgnoreCase("p")
					|| fieldType.equalsIgnoreCase("div")) {
				valueOfElement = userNameElement.getText();
			}

			logger.debug("Page title is: " + driver.getTitle());
			if (status.equalsIgnoreCase("Initial")) {
				if (!com.rsi.utils.RsiTestingHelper.checkEmpty(valueOfElement)
						|| !com.rsi.utils.RsiTestingHelper.checkEmpty(readElement)) {
					if (readElement.startsWith("{") && readElement.endsWith("}")) {
						// We know that we need to check a regular expression
						if (valueOfElement.matches(readElement.substring(1, readElement.length() - 1))) {
							status = "Success";
						} else {
							status = "Failure";
							description = description.concat(" - No read element or value onthe page to inspect");
						}
					} else {
						if (valueOfElement.equalsIgnoreCase(readElement)) {
							status = "Success";
						} else {
							status = "Failure";
							description = description.concat(" - valueOfElement was [" + valueOfElement
									+ "],  expected value was [" + readElement + "]");
						}
					}

				} else {
					status = "Success";
				}
				logger.info("Element found " + fieldName + " successfully.");
			}
		} catch (NoSuchElementException nse) {
			nse.printStackTrace();
			logger.error("Error no element found on the page " + nse.getMessage());
			description = description.concat(" - " + nse.getMessage());
			status = "Failure";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error other exception " + e.getMessage());
			description = description.concat(" - " + e.getMessage());
			status = "Failure";
		} finally {
			driver.switchTo().defaultContent();
		}

		return status;
	}

	public String actionPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName,
			String fieldType, String readElement, String xpath, String action, String actionUrl, String baseURL,
			String initialDescription, int currentSchedulerId, int currentTestCaseId, int currentTestSequence,
			int resultSuiteId) {
		String status = "Initial";
		WebElement clickableElement = null;
		String description = initialDescription;

		try {
			String currentPageUrl = driver.getCurrentUrl();
			if (!RsiTestingHelper.checkEmpty(baseURL)) {
				if (!currentPageUrl.equalsIgnoreCase(baseURL) && currentTestSequence == 1)
					driver.get(baseURL);
			}

			try {
				clickableElement = fetchWebElement("ACTION", fieldName, xpath);
			} catch (NoSuchElementException nse) {
				// we will try one more time by checking all elements of the field type with
				// getText() of fieldName.
				List<WebElement> elements = driver.findElements(By.tagName(fieldType));
				for (WebElement e : elements) {
					if (e.getText().equalsIgnoreCase(fieldName)) {
						clickableElement = e;
					}
				}
			}
			// NEXT Section should only be called if the status has not been populated as
			// yet. Which in the case of anchor tag is already taken care of. (Sameer
			// 01262020)
			if (status.equalsIgnoreCase("initial") && clickableElement != null) {
				if (action.equalsIgnoreCase("Click")) {
					if (performAction(clickableElement)) {
						// clickableElement.click();
						if (!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl))
							status = checkStatus(url, readElement, actionUrl);
						else
							status = "Success";
					}

				} else if (action.equalsIgnoreCase("tab")) {
					clickableElement.sendKeys(Keys.TAB);
					if (!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl))
						status = checkStatus(url, readElement, actionUrl);
					else
						status = "Success";
				}
			} else {
				if (status.equalsIgnoreCase("initial"))
					status = "Failure";
			}

		} catch (NoSuchElementException nse) {
			nse.printStackTrace();
			logger.error("Error no element found on the page " + nse.getMessage());
			description = description.concat(" - " + nse.getMessage() + ".");
			status = "Failure";
		} catch (InterruptedException e) {
			logger.error("Error Interrupted exception " + e.getMessage());
			description = description.concat(" - " + e.getMessage() + ".");
			status = "Failure";
		} catch (Exception e) {
			logger.error("Error unknown exception " + e.getMessage());
			description = description.concat(" - " + e.getMessage() + ".");
			status = "Failure";
		} finally {
			driver.switchTo().defaultContent();
		}

		return status;
	}

	private boolean performAction(WebElement clickableElement) throws NoSuchElementException, InterruptedException {
		boolean bRetStatus = false;
		try {
			clickableElement.click();
		} catch (WebDriverException wde) {
			Actions action = new Actions(driver);
			action.moveToElement(clickableElement).click().perform();
		}
		bRetStatus = true;

		return bRetStatus;
	}

	private String checkStatus(String url, String readElement, String actionUrl) {
		if (!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl)) {
			if (actionUrl.equals(driver.getCurrentUrl())) {
				return "Success";
			} else {
				// sometimes the actionURL is present because of a Ajax call inside a div
				// window.
				// hence no need to switch tab. in those situations we can return success.
				Set<String> handles = driver.getWindowHandles();
				if (handles.size() == 1) {
					// no new tabs exist. assuming that this is a new url in the same tab.
					return "Success";
				}
				logger.debug("In check status method returning false since actionUrl [" + actionUrl
						+ "] is not the same as the url [" + driver.getCurrentUrl() + "]");
				// Maybe it is because the new page has opened on a new tab. Switch tab and see
				// if the currentUrl in that tab is the same as actionUrl
				switchToNewTab();
				if (actionUrl.equals(driver.getCurrentUrl())) {
					return "Success";
				} else {
					return "Failure";
				}
			}
		}

		return "Success";
	}

	public String inputPageElement(Connection conn, String url, String loginName, String loginPwd, String fieldName,
			String field_type, String inputValue, String xpath, String base_url, String need_screenshot,
			String initialDescription, String enterAction, int currentSchedulerId, int currentTestCaseId,
			int currentTestSequence, int resultSuiteId) {
		String status = "Failure";
		WebElement element = null;
		String currentPageUrl = driver.getCurrentUrl();
		String description = initialDescription;
		try {
			if (!currentPageUrl.equalsIgnoreCase(base_url) && currentTestSequence == 1)
				driver.get(base_url);

			element = fetchWebElement("INPUT", fieldName, xpath);

			// TODO this is failing for select.
			// element.clear();
			element.sendKeys(inputValue);
			// (SAMEER 05302020) this can be an additional switch on an input field, if we
			// should select the inputted value esp. in cases where inputted value has to be
			// qualified from a dynamic list.
			TimeUnit.SECONDS.sleep(2);
			if (enterAction.equalsIgnoreCase("1")) {
				element.sendKeys(Keys.ENTER);
			}
			status = checkStatus(url, fieldName, "");
		} catch (NoSuchElementException nse) {
			nse.printStackTrace();
			logger.error("Error no element found on the page " + nse.getMessage());
			description = description.concat(" - " + nse.getMessage() + ".");
			status = "Failure";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error other exception " + e.getMessage());
			description = description.concat(" - " + e.getMessage() + ".");
			status = "Failure";
		} finally {
			// TODO if switched to a iframe switch back to the main window.
			driver.switchTo().defaultContent();

		}
		return status;
	}

	private WebElement fetchWebElement(String fieldCategory, String fieldName, String xpath)
			throws NoSuchElementException, InterruptedException {
		WebElement element = null;
		String[] xpathColl = null;

		if (hasSwitch(xpath)) {
			xpathColl = xpath.split("<switch>");
			// TimeUnit.SECONDS.sleep(2);
			driver.switchTo().frame(driver.findElement(By.xpath(xpathColl[0])));
		}
		if ((fieldName == null || fieldName.trim().length() == 0)) {
			if (hasSwitch(xpath)) {
				element = driver.findElement(By.xpath(xpathColl[1]));
			} else {
				element = driver.findElement(By.xpath(xpath));
			}
		} else {
			// SAMEER - 09212020 changed By.id to By.name. In addition to this change, we
			// also need to make sure if this next line fails it is handled by some other
			// means
			try {
				element = driver.findElement(By.id(fieldName));
			} catch (NoSuchElementException nse) {
				// try {
				// element = driver.findElement(By.name(fieldName));
				// } catch (Exception e) {
				// logger.error("Could not find the element with the fieldName " + fieldName + "
				// now check for XPath by calling the same method");
				fetchWebElement(fieldCategory, null, xpath);
				// }
			}
		}
		return element;
	}

	private boolean hasSwitch(String xpath) {
		boolean bRetval = false;
		if (xpath != null) {
			if (xpath.contains("<switch>")) {
				bRetval = true;
			}
		}
		return bRetval;
	}

	public String takeScreenshot(Connection conn, long resultCaseId) {
		String fileName = new Long(resultCaseId).toString() + ".png";
		try {
			TakesScreenshot ts = (TakesScreenshot) driver;

			File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			FileUtils.copyFile(source, new File("./Screenshots/" + fileName));
			logger.info("Screenshot taken");
			// Now save the file name to the database.
			PreparedStatement pstmt = conn
					.prepareStatement("UPDATE result_cases SET screenshot_file_location = ? WHERE id = ?");
			pstmt.setString(1, fileName);
			pstmt.setLong(2, resultCaseId);

			pstmt.execute();

		} catch (Exception e) {
			logger.error("Exception while taking screenshot " + e.getMessage());
		}
		return fileName;
	}

	public boolean switchToNewTab() {
		ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles());
		logger.debug("number of tabs are " + tabs.size());
		driver.switchTo().window(tabs.get(tabs.size() - 1));

		return true;
	}
}
