package com.rsi.selenium;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.rsi.dataObject.TestResult;
import com.rsi.utils.RsiTestingHelper;
import org.apache.commons.io.FileUtils;

import org.apache.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RsiTester {
	final static Logger logger = Logger.getLogger(RsiTester.class);
	private WebDriver driver;

	public WebDriver getDriver() {
		return driver;
	}

	public RsiTester(String runType, String browserType) {
		ResourceBundle rb = ResourceBundle.getBundle("rstester");
		if (browserType == BrowserType.FIREFOX) {
			System.setProperty("webdriver.gecko.driver", rb.getString("FIREFOX_PATH"));
			FirefoxOptions options = new FirefoxOptions();
			options.setHeadless(runType == "headless");
			driver = new FirefoxDriver(options);
		} else {
			ChromeOptions options = new ChromeOptions();

			System.setProperty("webdriver.chrome.driver", rb.getString("CHROME_PATH"));
			options.addArguments("--" + runType);
			driver = new ChromeDriver(options);
		}
	}

	public TestResult loginToApp(String url_to_test, String userNameField, String passwordField, String btnField,
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

		TestResult res = new TestResult();
		res.setStatus(status);
		return res;
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

	public TestResult testPageElement(String fieldName, String xpath, String fieldType, String readElement,
			String initialDescription, int currentTestSequence) {
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

			fieldType = fieldType.toLowerCase();

			String[] valueTags = new String[] { "label", "text", "textArea" };

			String[] textTags = new String[] { "span", "th", "td", "li", "p", "div", "h1", "h2", "h3", "h4", "h5", "h6" };

			if (Arrays.asList(valueTags).contains(fieldType)) {
				valueOfElement = userNameElement.getAttribute("value");
			} else if (Arrays.asList(textTags).contains(fieldType)) {
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

		TestResult res = new TestResult();
		res.setStatus(status);
		res.setDescription(description);
		return res;
	}

	public TestResult actionPageElement(String fieldName, String fieldType, String xpath, String action,
			String actionUrl, String inputValue, String baseURL, String initialDescription, int currentTestSequence) {
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
				System.out.println("NoSuchElementException ACTION: Trying again");
				if (!RsiTestingHelper.checkEmpty(fieldType) && !RsiTestingHelper.checkEmpty(fieldName)) {
					List<WebElement> elements = driver.findElements(By.tagName(fieldType));
					for (WebElement e : elements) {
						if (e.getText().equalsIgnoreCase(fieldName)) {
							clickableElement = e;
						}
					}
				}
			}
			// NEXT Section should only be called if the status has not been populated as
			// yet. Which in the case of anchor tag is already taken care of. (Sameer
			// 01262020)
			if (status.equalsIgnoreCase("initial") && clickableElement != null) {
				if (fieldType.equalsIgnoreCase("file") && !RsiTestingHelper.checkEmpty(inputValue)) {
					clickableElement.sendKeys(inputValue);
				} else if (action.equalsIgnoreCase("Click")) {
					if (performAction(clickableElement)) {
						if (!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl))
							status = checkStatus(actionUrl);
						else
							status = "Success";
					}

				} else if (action.equalsIgnoreCase("tab")) {
					clickableElement.sendKeys(Keys.TAB);
					if (!com.rsi.utils.RsiTestingHelper.checkEmpty(actionUrl))
						status = checkStatus(actionUrl);
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

		TestResult res = new TestResult();
		res.setStatus(status);
		res.setDescription(description);
		return res;
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

	private String checkStatus(String actionUrl) {
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

	private void setAttributeValue(WebElement element, String attribute, String value) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].setAttribute(arguments[1],arguments[2])", element, attribute, value);
	}

	public TestResult inputPageElement(String fieldName, String inputValue, String xpath, String base_url,
			String initialDescription, String enterAction, int currentTestSequence) {
		String status = "Failure";
		WebElement element = null;
		String currentPageUrl = driver.getCurrentUrl();
		String description = initialDescription;
		try {
			if (!RsiTestingHelper.checkEmpty(base_url)) {
				if (!currentPageUrl.equalsIgnoreCase(base_url) && currentTestSequence == 1)
					driver.get(base_url);
			}

			element = fetchWebElement("INPUT", fieldName, xpath);
			String isReadOnly = element.getAttribute("readonly");
			if (isReadOnly != null && isReadOnly.equalsIgnoreCase("true")) {
				setAttributeValue(element, "readonly", "0");
				setAttributeValue(element, "value", inputValue);
			} else {
				element.sendKeys(inputValue);
			}
			status = "Success";
			// TODO this is failing for select.
			// element.clear();

			// (SAMEER 05302020) this can be an additional switch on an input field, if we
			// should select the inputted value esp. in cases where inputted value has to be
			// qualified from a dynamic list.

			if (enterAction.equalsIgnoreCase("1")) {
				TimeUnit.SECONDS.sleep(2);
				element.sendKeys(Keys.ENTER);
			}
			status = checkStatus("");
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
			// if switched to a iframe switch back to the main window.
			driver.switchTo().defaultContent();

		}
		TestResult res = new TestResult();
		res.setStatus(status);
		res.setDescription(description);
		return res;
	}

	private WebElement fetchWebElement(String fieldCategory, String fieldName, String xpath)
			throws NoSuchElementException, InterruptedException {
		WebElement element = null;

		String elementXPath = xpath;

		if (hasSwitch(xpath)) {
			String[] xpathColl = null;
			xpathColl = xpath.split("<switch>");

			for (Integer i = 0; i < xpathColl.length - 1; i++) {
				driver.switchTo().frame(driver.findElement(By.xpath(xpathColl[i])));
			}

			elementXPath = xpathColl[xpathColl.length - 1];
		}

		Integer recheckCounter = 0;

		while (element == null && recheckCounter <= 2) {
			try {
				if ((fieldName == null || fieldName.trim().length() == 0)) {
					element = driver.findElement(By.xpath(elementXPath));
				} else {
					// SAMEER - 09212020 changed By.id to By.name. In addition to this change, we
					// also need to make sure if this next line fails it is handled by some other
					// means
					try {
						element = driver.findElement(By.id(fieldName));
					} catch (NoSuchElementException nse) {
						element = fetchWebElement(fieldCategory, null, elementXPath);
					} finally {
						recheckCounter++;
					}
				}
			} catch (NoSuchElementException nse) {
				try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					recheckCounter++;
				}
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

	public void goToLandingPage(String urlToTest) {
		driver.get(urlToTest);
	}
}
