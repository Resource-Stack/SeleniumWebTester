package com.rsi.selenium;

import com.rsi.dataObject.CustomCommand;
import com.rsi.dataObject.H2OApplication;
import com.rsi.dataObject.TestCase;
import com.rsi.dataObject.TestResult;
import com.rsi.selenium.factory.H2OTesterConnectionFactory;
import com.rsi.utils.EmailManagementUtility;

import org.apache.log4j.Logger;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.openqa.selenium.NoSuchElementException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RsitesterMain {
	final static Logger logger = Logger.getLogger(RsitesterMain.class);

	// ARGS: mode ["headless"/"start-maximized"], schedulerID
	public static void main(String[] args) {
		H2OApplication curApp = null;

		// STEP 1: GET Database Connection
		H2OTesterConnectionFactory appFactory = H2OTesterConnectionFactory.getInstance();
		Connection conn = appFactory.getDatabaseConnection();

		Statement stmt = null;
		ResultSet scheduledSet = null;

		String chromeMode = args.length == 0 ? "start-maximized" : args[0];

		// STEP 2: Read the scheduler table.
		try {
			stmt = conn.createStatement();
			int schedulerID = args.length == 2 ? Integer.parseInt(args[1]) : -1;
			// Search based on schedulerID
			scheduledSet = findResultFromSchedulerID(conn, stmt, schedulerID);

			while (scheduledSet != null && scheduledSet.next()) {
				int currentSchedulerId = scheduledSet.getInt("id");
				int currentSuiteId = scheduledSet.getInt("test_suite_id");
				int numberOfTimesToRun = scheduledSet.getInt("number_of_times");

				if (numberOfTimesToRun > 1) {
					chromeMode = "headless";
				}

				if (curApp == null) {
					logDebugMessage(
							"Trying to fetch the enviroment for id [ " + scheduledSet.getInt("environment_id") + " ]");
					curApp = appFactory.getApplicationEnvironment(scheduledSet.getInt("environment_id"));
				}
				if (curApp == null) {
					logDebugMessage("Error could not find an environment setting for scheduled job [ "
							+ scheduledSet.getString("id") + " ], cannot run test suite id [ "
							+ scheduledSet.getInt("environment_id") + " ], moving to the next scheduled job...");
					continue;
				}

				Boolean schedulerUpdated = updateSchedulerStatus(conn, currentSchedulerId, "Running");
				if (!schedulerUpdated) {
					logInfoMessage("Cannot attain a lock on this scheduler id [" + currentSchedulerId
							+ "] object. QUITTING...");
					return;
				}

				PreparedStatement pstmt = conn.prepareStatement(
						"SELECT tc.id as id, tc.field_name as field_name, tc.field_type as field_type, tc.read_element as read_element, tc.xpath as xpath, tc.input_value as input_value, tc.string as string, tc.action as action, ts.base_url as base_url, tc.action_url as action_url, tc.sleeps as sleeps, cs.sequence as sequence, tc.new_tab as new_tab, tc.need_screenshot as need_screenshot, tc.description as description, tc.enter_action as enter_action FROM test_cases tc, case_suites cs, test_suites ts WHERE cs.test_case_id = tc.id AND ts.id = cs.test_suite_id AND cs.test_suite_id = ? ORDER BY cs.sequence");

				pstmt.setInt(1, scheduledSet.getInt("test_suite_id"));
				boolean casesRetrieved = pstmt.execute();

				if (!casesRetrieved) {
					return;
				}
				ResultSet testCaseResult = pstmt.getResultSet();

				ArrayList<TestCase> testCases = new ArrayList<TestCase>();
				while (testCaseResult != null && testCaseResult.next()) {
					TestCase testCase = new TestCase();
					testCase.setId(testCaseResult.getInt("id"));
					testCase.setfieldName(testCaseResult.getString("field_name"));
					testCase.setfieldType(testCaseResult.getString("field_type"));
					testCase.setReadElement(testCaseResult.getString("read_element"));
					testCase.setXPath(testCaseResult.getString("xpath"));
					testCase.setInputValue(testCaseResult.getString("input_value"));
					testCase.setAction(testCaseResult.getString("action"));
					testCase.setBaseUrl(testCaseResult.getString("base_url"));
					testCase.setActionUrl(testCaseResult.getString("action_url"));
					testCase.setSleeps(testCaseResult.getString("sleeps"));
					testCase.setSequence(testCaseResult.getInt("sequence"));
					testCase.setNewTab(testCaseResult.getString("new_tab"));
					testCase.setNeedScreenshot(testCaseResult.getString("need_screenshot"));
					testCase.setDescription(testCaseResult.getString("description"));
					testCase.setEnterAction(testCaseResult.getString("enter_action"));
					testCases.add(testCase);
				}

				ExecutorService threadPool = Executors.newFixedThreadPool(numberOfTimesToRun);

				ArrayList<String> statuses = new ArrayList<String>();
				final String chromiumMode = chromeMode;
				final H2OApplication app = curApp;
				final ResultSet rs = scheduledSet;
				ArrayList<RsiChromeTester> chromeTesters = new ArrayList<RsiChromeTester>();
				final ArrayList<TestCase> testCaseList = testCases;
				for (int i = 0; i < numberOfTimesToRun; i++) {
					final Integer schedulerIndex = i + 1;
					threadPool.submit(new Runnable() {
						public void run() {
							TestResult res = new TestResult();
							RsiChromeTester chromeTester = null;
							Integer resultSuiteId = RsitesterMain.createNewResultSuite(conn, currentSchedulerId,
									currentSuiteId, schedulerIndex);
							try {
								chromeTester = new RsiChromeTester(chromiumMode);
								chromeTesters.add(chromeTester);

								try {
									if (app.getLoginRequired()) {
										chromeTester.loginToApp(app.getUrl(), app.getLoginField(),
												app.getPasswordField(), app.getActionButton(), app.getLoginName(),
												app.getLoginPwd(), "success_field");
									} else {
										chromeTester.goToLandingPage(app.getUrl());
									}
								} catch (NoSuchElementException nse) {
									logErrorMessage("Element not found Error [ " + nse.getMessage() + " ]");
									updateSchedulerStatus(conn, currentSchedulerId, "Error");
									RsitesterMain.updateResultSuite(conn, resultSuiteId, 2);
									return;
								} catch (Exception e) {
									e.printStackTrace();
									RsitesterMain.updateResultSuite(conn, resultSuiteId, 2);
								}

								logDebugMessage("Now starting to process Scheduled Job Id [ " + rs.getString("id")
										+ " ], this job was scheduled on [ " + rs.getString(3) + " ], suite id is [ "
										+ rs.getInt("test_suite_id") + " ]");

								if (casesRetrieved) {

									for (int j = 0; j < testCaseList.size(); j++) {
										TestCase curCase = testCaseList.get(j);
										Boolean caseSuccess = true;
										int currentTestCaseId = curCase.getId();
										int currentTestSequence = curCase.getSequence();
										logDebugMessage("Now running test case [ " + currentTestCaseId
												+ " ], for field name [ " + curCase.getFieldName()
												+ " ] and the sequence is [" + currentTestSequence + "]");

										String testCaseType = identifyTestCase(curCase.getfieldType(),
												curCase.getInputValue(), curCase.getAction());

										logDebugMessage(
												"TestCaseId " + currentTestCaseId + " identified as " + testCaseType);

										if (testCaseType == "INSPECT") {
											try {
												res = chromeTester.testPageElement(curCase.getFieldName(),
														curCase.getXPath(), curCase.getfieldType(),
														curCase.getReadElement(), curCase.getDescription(),
														currentTestSequence);
												if (res.getStatus().equalsIgnoreCase("Failure")) {
													caseSuccess = false;
												}
												sleepIfInstructedTo(curCase.getSleeps());
											} catch (NoSuchElementException nse) {
												logErrorMessage(nse.getMessage());
												caseSuccess = false;
											} catch (InterruptedException ie) {
												logErrorMessage(ie.getMessage());
												caseSuccess = false;
												ie.printStackTrace();
											}
										} else if (testCaseType == "ACTION") {
											try {
												res = chromeTester.actionPageElement(curCase.getFieldName(),
														curCase.getfieldType(), curCase.getXPath(), curCase.getAction(),
														curCase.getActionUrl(), curCase.getInputValue(),
														curCase.getBaseUrl(), curCase.getDescription(),
														currentTestSequence);
												sleepIfInstructedTo(curCase.getSleeps());
											} catch (NoSuchElementException nse) {
												logErrorMessage(nse.getMessage());
												caseSuccess = false;
											} catch (InterruptedException ie) {
												logErrorMessage(ie.getMessage());
												caseSuccess = false;
												ie.printStackTrace();
											}
										} else if (testCaseType == "INPUT") {
											try {
												res = chromeTester.inputPageElement(curCase.getFieldName(),
														curCase.getInputValue(), curCase.getXPath(),
														curCase.getBaseUrl(), curCase.getDescription(),
														curCase.getEnterAction(), currentTestSequence);
												sleepIfInstructedTo(curCase.getSleeps());
											} catch (NoSuchElementException nse) {
												logErrorMessage(
														"Error when handling Input type case... " + nse.getMessage());
												caseSuccess = false;
											} catch (InterruptedException ie) {
												logErrorMessage(ie.getMessage());
												caseSuccess = false;
											}
										} else if (testCaseType == "CUSTOM") {
											// TODO now try to instantiate custom application code to execute backend
											// methods that could not be performed from frontend. such as cleanup an
											// object.
											// Now assume that a custom class has been implemented for this application.
											logInfoMessage("Now inside custom code.");
											String commandName = curCase.getFieldName().substring(1,
													curCase.getFieldName().length() - 1);
											// CommonHealthCore_Dev chcDev = new CommonHealthCore_Dev(app, conn);
											for (CustomCommand command : app.getCustomCommands()) {
												if (commandName.equalsIgnoreCase(command.getCustomCommandName())) {
													logDebugMessage("Found the command to run " + command.toString());
													try {
														String all_params = "";
														all_params = buildParamString(command.getParams(),
																curCase.getReadElement());
														Process p = Runtime.getRuntime()
																.exec(command.getCustomCommand() + " " + all_params);
														if (!p.waitFor(1, TimeUnit.MINUTES)) {
															InputStreamReader ssReader = new InputStreamReader(
																	p.getInputStream());

															BufferedReader reader = new BufferedReader(ssReader);
															StringBuffer sb = new StringBuffer();
															String str;
															while ((str = reader.readLine()) != null) {
																sb.append(str);
															}
															if (com.rsi.utils.RsiTestingHelper
																	.checkEmpty(sb.toString())) {
																res.setStatus("Failure");
																caseSuccess = true;
															} else {
																logInfoMessage("Return from Custom Script [ "
																		+ sb.toString() + " ]");
																res.setStatus("Success");
															}
														} else {
															p.destroy(); // consider using destroyForcibly instead
														}
													} catch (IOException ioe) {
														res.setStatus("Failure");
														caseSuccess = false;
													} catch (RuntimeException re) {
														res.setStatus("Failure");
														caseSuccess = false;
														logErrorMessage("re.getMessage()");
													} catch (InterruptedException ine) {
														res.setStatus("Failure");
														caseSuccess = false;
														logErrorMessage("ine.getMessage()");
													}
												}

											}
										}
										if (res.getStatus().equalsIgnoreCase("Failure")) {
											caseSuccess = false;
										}
										Integer resultCaseId = createNewResultCase(conn, currentTestCaseId,
												currentSchedulerId, resultSuiteId, res.getDescription(),
												com.rsi.utils.RsiTestingHelper.returmTimeStamp(),
												com.rsi.utils.RsiTestingHelper.returmTimeStamp(), caseSuccess);

										String need_screenshot = curCase.getNeedScreenshot();
										if (!caseSuccess
												|| !com.rsi.utils.RsiTestingHelper.checkEmpty(need_screenshot)) {
											if (!caseSuccess || need_screenshot.equalsIgnoreCase("1")) {
												chromeTester.takeScreenshot(conn, resultCaseId);
											}
										}

										logDebugMessage("Status returned is [ " + res.getStatus() + " ]");
										if (!com.rsi.utils.RsiTestingHelper.checkEmpty(curCase.getNewTab())
												&& curCase.getNewTab().equalsIgnoreCase("1")) {
											chromeTester.switchToNewTab();
											try {
												TimeUnit.SECONDS.sleep(5);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
										}

										statuses.add(res.getStatus());
									}

									RsitesterMain.updateResultSuite(conn, resultSuiteId,
											res.getStatus() == "Failure" ? 2 : 1);
								}

							} catch (SQLException e1) {
								e1.printStackTrace();
							}
						}
					});
				}

				threadPool.shutdown();
				try {
					threadPool.awaitTermination(300, TimeUnit.SECONDS);
					if (!statuses.contains("Failure") && statuses.contains("Success")) {
						updateSchedulerStatus(conn, currentSchedulerId, "Complete");
					} else {
						updateSchedulerStatus(conn, currentSchedulerId, "Error");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					chromeTesters.forEach(t -> {
						if (t != null)
							t.getDriver().quit();
					});
					// send out an email to the recipient of this environment.
					EmailManagementUtility.sendEmail(app, currentSchedulerId, conn);
				}

			}
		} catch (SQLException e) {
			logErrorMessage(
					"Looks like Something bad happened while running the query, most likely referential data does not exist.");
			logErrorMessage(e.getMessage());
		} finally {
			if (scheduledSet != null) {
				try {
					scheduledSet.close();
				} catch (SQLException sqlEx) {
				} finally {
					scheduledSet = null;
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) {
				} finally {
					stmt = null;
				}
			}
		}
	}

	private static ResultSet findResultFromSchedulerID(Connection conn, Statement stmt, Integer schedulerID)
			throws SQLException {
		ResultSet rs = null;
		rs = stmt.executeQuery(
				"SELECT s.id id, s.test_suite_id, s.scheduled_date, s.number_of_times, t.environment_id environment_id FROM schedulers s, test_suites t WHERE s.test_suite_id = t.id AND s.id = '"
						+ schedulerID.toString() + "' ORDER BY s.updated_at DESC LIMIT 1");

		return rs;
	}

	private static Boolean updateResultSuite(Connection conn, int resultSuiteId, int status) {
		PreparedStatement pstmt = null;
		boolean success = false;
		try {
			pstmt = conn.prepareStatement("UPDATE result_suites SET rd_id = ?, end_time = ? WHERE id = ?");
			pstmt.setInt(1, status); // Complete = 1, Error = 2, Running = 3
			pstmt.setString(2, status == 3 ? "" : com.rsi.utils.RsiTestingHelper.returmTimeStamp());
			pstmt.setInt(3, resultSuiteId);
			success = pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (success) {
			logInfoMessage("Updated Result Suite with id " + resultSuiteId + " to rd_id " + status);
		} else {
			logErrorMessage("Failed to Result Suite with id " + resultSuiteId + " to rd_id " + status);
		}
		return success;
	}

	private static Integer createNewResultSuite(Connection conn, int currentSchedulerId, int currentSuiteId,
			int schedulerIndex) {
		Integer resultSuiteId = -1;
		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement(
					"INSERT INTO result_suites (rd_id, scheduler_id, test_suite_id, scheduler_index, start_time) VALUES(?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, 3); // Running
			pstmt.setInt(2, currentSchedulerId);
			pstmt.setInt(3, currentSuiteId);
			pstmt.setInt(4, schedulerIndex);
			pstmt.setString(5, com.rsi.utils.RsiTestingHelper.returmTimeStamp());

			if (pstmt.execute()) {
				logInfoMessage("Created new result suite.");
			} else {
				logErrorMessage("Could not create a new result suite.");
			}
			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				resultSuiteId = rs.getInt(1);
			}

		} catch (SQLException e) {
			logErrorMessage("Could not create a new result suite.");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return resultSuiteId;
	}

	private static Integer createNewResultCase(Connection conn, int currentTestCaseId, int currentSchedulerId,
			int currentResultSuiteId, String description, String startTime, String endTime, Boolean success) {
		Integer resultCaseID = -1;
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(
					"INSERT INTO result_cases (rd_id,test_case_id,scheduler_id, error_description, created_at, updated_at, result_suite_id) VALUES (?, ?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, success ? 1 : 2);
			pstmt.setInt(2, currentTestCaseId);
			pstmt.setInt(3, currentSchedulerId);
			pstmt.setString(4, description);
			pstmt.setString(5, startTime);
			pstmt.setString(6, endTime);
			pstmt.setInt(7, currentResultSuiteId);

			pstmt.execute();
			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				resultCaseID = rs.getInt(1);
				logInfoMessage("Updated TestCase Result id [" + currentTestCaseId + " ]");
			} else {
				logErrorMessage("Could not update the Test Case id in Results [" + currentTestCaseId
						+ " ]. Please delete it manually. ");
			}
		} catch (SQLException e) {
			logErrorMessage("Could not update the Test Case with Result for id [" + currentTestCaseId
					+ " ]. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return resultCaseID;
	}

	private static Boolean updateSchedulerStatus(Connection conn, int currentSchedulerId, String status) {
		PreparedStatement pstmt = null;
		boolean success = false;
		try {
			pstmt = conn.prepareStatement("UPDATE schedulers SET status = ? WHERE id = ?");
			pstmt.setString(1, status);
			pstmt.setInt(2, currentSchedulerId);
			success = pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (success) {
			logInfoMessage("Updated Scheduler id [" + currentSchedulerId + " ], with " + status);
		} else {
			logErrorMessage("Could not update the Scheduler id [" + currentSchedulerId + " ] with " + status
					+ " Status. Please delete it manually. ");
		}
		return success;
	}

	private static String identifyTestCase(String fieldType, String inputValue, String action) {
		if (!com.rsi.utils.RsiTestingHelper.checkEmpty(fieldType) && fieldType.startsWith("[")
				&& fieldType.endsWith("]")) {
			return "CUSTOM";
		}
		if (!com.rsi.utils.RsiTestingHelper.checkEmpty(action) && !action.equalsIgnoreCase("read")) {
			return "ACTION";
		}
		if (!com.rsi.utils.RsiTestingHelper.checkEmpty(action) && action.equalsIgnoreCase("read")) {
			return "INSPECT";
		}

		// string - field_type ----- string2 - input_value ----------- string3 - action
		if (fieldType.equalsIgnoreCase("anchor") || fieldType.equalsIgnoreCase("a")
				|| fieldType.equalsIgnoreCase("span") || fieldType.equalsIgnoreCase("select")
				|| fieldType.equalsIgnoreCase("option")) {
			if (!com.rsi.utils.RsiTestingHelper.checkEmpty(action) || !action.equalsIgnoreCase("Read")) {
				if (com.rsi.utils.RsiTestingHelper.checkEmpty(action)
						&& !com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue)) {
					return "INPUT";
				} else {
					return "ACTION";
				}
			} else {
				return "INSPECT";
			}
		} else if (com.rsi.utils.RsiTestingHelper.checkEmpty(action) && fieldType.equalsIgnoreCase("label")) {
			return "INSPECT";
		} else if (fieldType.equalsIgnoreCase("checkbox") || fieldType.equalsIgnoreCase("radio")) {
			if (!com.rsi.utils.RsiTestingHelper.checkEmpty(action)) {
				return "ACTION";
			} else {
				return "INSPECT";
			}
		} else if (fieldType.equalsIgnoreCase("button")) {
			if (!com.rsi.utils.RsiTestingHelper.checkEmpty(action)) {
				return "ACTION";
			} else {
				return "INSPECT";
			}

		} else if (fieldType.equalsIgnoreCase("text") || fieldType.equalsIgnoreCase("textarea")
				|| fieldType.equalsIgnoreCase("search")) {
			if (!com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue)) {
				return "INPUT";
			} else if (com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue) && action == null) {
				return "INSPECT";
			} else if (com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue) && action != null) {
				return "ACTION";
			}
		}

		return "INSPECT";
	}

	private static void sleepIfInstructedTo(String sleep) throws InterruptedException {
		if (!com.rsi.utils.RsiTestingHelper.checkEmpty(sleep)) {
			try {
				int iSleepTimeInSecs = Integer.parseInt(sleep);
				TimeUnit.SECONDS.sleep(iSleepTimeInSecs);
			} catch (NumberFormatException nfe) {
				logDebugMessage("Could not convert the sleep time to a number sleep time set was [ " + sleep + " ]");
				TimeUnit.SECONDS.sleep(15);
			}
		}
	}

	private static String buildParamString(String[] params, String read_element) {
		ArrayList<String> outputParams = new ArrayList<String>();
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(read_element);
			logDebugMessage("JSON Object retrieved is " + jsonObject.toString());
			for (String param : params) {
				try {
					outputParams.add((String) jsonObject.get(param));
				} catch (NullPointerException npe) {
					npe.printStackTrace();
					logErrorMessage("no record found " + npe.getMessage());
				}

			}
		} catch (JSONException err) {
			logErrorMessage(err.toString());
			return "";
		}
		String csv = outputParams.toString().replace("[", "").replace("]", "").replace(", ", " ");
		logDebugMessage("csv is [" + csv + "]");
		return csv;
	}

	private static void logErrorMessage(Object message) {
		logger.error(message);
	}

	private static void logDebugMessage(Object message) {
		logger.debug(message);
	}

	private static void logInfoMessage(Object message) {
		logger.info(message);
	}
}
