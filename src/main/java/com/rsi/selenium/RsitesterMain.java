package com.rsi.selenium;

import com.rsi.dataObject.CustomCommand;
import com.rsi.dataObject.H2OApplication;
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
import java.util.concurrent.TimeUnit;

public class RsitesterMain {
	final static Logger logger = Logger.getLogger(RsitesterMain.class);
	public static void main(String[] args) {
		Connection conn 		= null;
		Statement stmt 			= null;
		PreparedStatement pstmt	= null;
		ResultSet rsForTestCases= null;
		ResultSet rs 			= null;
		H2OApplication app		= null;
		
		RsiChromeTester chromeTester = new RsiChromeTester();
		
		
		String status 		= "Failed";
		
		// STEP 1: GET Database Connection
		H2OTesterConnectionFactory appFactory = H2OTesterConnectionFactory.getInstance();
		
		conn = appFactory.getDatabaseConnection();
		
		// STEP 2: Read the scheduler table. 
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT s.id id, s.test_suite_id, s.scheduled_date, t.environment_id environment_id FROM schedulers s, test_suites t WHERE s.test_suite_id = t.id AND s.status = 'READY' ORDER BY s.updated_at DESC LIMIT 1");
			
			while (rs.next()) {
				int currentSchedulerId = rs.getInt("id");
				int currentSuiteId = rs.getInt("test_suite_id");
				if (app == null) {
					logger.debug("Trying to fetch the enviroment for id [ " + rs.getInt("environment_id") + " ]");
					app = appFactory.getApplicationEnvironment(rs.getInt("environment_id"));
				}
				if (app == null) {
					logger.debug("Error could not find an environment setting for scheduled job [ " + rs.getString("id") + " ], cannot run test suite id [ " + rs.getInt("environment_id") + " ], moving to the next scheduled job...");
					continue;
				}
				else {
					//chromeTester.loginToApp(app.getUrl(), "CacheUserName", "CachePassword", "btnSearch", app.getLoginName(), app.getLoginPwd(), "success_field");
					logger.debug("app returned is [ " + app.toString() + " ]");
					try {
					//chromeTester.loginToApp(app.getUrl(), "CacheUserName", "CachePassword", "btnSearch", app.getLoginName(), app.getLoginPwd(), "success_field");
					chromeTester.loginToApp(app.getUrl(), app.getLoginField(), app.getPasswordField(), app.getActionButton(), app.getLoginName(), app.getLoginPwd(), "success_field");
					} catch (NoSuchElementException nse) {
						logger.error("Element not found Error [ " + nse.getMessage() + " ]");
						updateSchedulerWithError(conn, currentSchedulerId, currentSuiteId, app);
						continue;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if(!updateSchedulerWithRunning(conn, currentSchedulerId, currentSuiteId, app)) {
					logger.info("Cannot attain a lock on this scheduler id [" + currentSchedulerId + "] object. QUITTING...");
					chromeTester.getDriver().quit();
					return;
				}
				logger.debug("Now starting to process Scheduled Job Id [ " + rs.getString("id")+ " ], this job was scheduled on [ " + rs.getString(3) + " ], suite id is [ " + rs.getInt("test_suite_id") + " ]");
				// Now try to access all the test cases for the test_suite_id submitted for this run.
				pstmt = conn.prepareStatement("SELECT tc.id as id, tc.field_name as field_name, tc.field_type as field_type, tc.read_element as read_element, tc.xpath as xpath, tc.input_value as input_value, tc.string as string, tc.action as action, ts.base_url as base_url, tc.action_url as action_url, tc.sleeps as sleeps, cs.sequence as sequence, tc.new_tab as new_tab, tc.need_screenshot as need_screenshot, tc.description as description, tc.enter_action as enter_action FROM test_cases tc, case_suites cs, test_suites ts WHERE cs.test_case_id = tc.id AND ts.id = cs.test_suite_id AND cs.test_suite_id = ? ORDER BY cs.sequence");
				pstmt.setInt(1, rs.getInt("test_suite_id"));
				if (pstmt.execute() == true) {
					rsForTestCases = pstmt.getResultSet();
					while (rsForTestCases.next()) {
						int currentTestCaseId = rsForTestCases.getInt("id");
						int currentTestSequence = rsForTestCases.getInt("sequence");
						logger.debug("Now running test case [ " + rsForTestCases.getString("id") + " ], for field name [ " + rsForTestCases.getString("field_name") +" ] and the sequence is [" + currentTestSequence + "]");
						if (identifyTestCase(rsForTestCases.getString("field_type"), rsForTestCases.getString("input_value"),
								rsForTestCases.getString("action")) == "INSPECT") {
								logger.debug("testcaseId " + currentTestCaseId + " identified as INSPECT");
							try {
								status = chromeTester.testPageElement(conn, app.getUrl(), app.getLoginName(), app.getLoginPwd(), rsForTestCases.getString("field_name"), rsForTestCases.getString("xpath"), rsForTestCases.getString("field_type"), rsForTestCases.getString("read_element"), rsForTestCases.getString("need_screenshot"), rsForTestCases.getString("description"), currentSchedulerId, currentTestCaseId, currentTestSequence);
								sleepIfInstructedTo(rsForTestCases.getString("sleeps"));
							}catch (NoSuchElementException nse) {
								logger.error(nse.getMessage());
								updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, com.rsi.utils.RsiTestingHelper.returmTimeStamp(), com.rsi.utils.RsiTestingHelper.returmTimeStamp());
								//nse.printStackTrace();
								continue;
							}catch(InterruptedException ie) {
								logger.error(ie.getMessage());
								updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, com.rsi.utils.RsiTestingHelper.returmTimeStamp(), com.rsi.utils.RsiTestingHelper.returmTimeStamp());
								ie.printStackTrace();
								continue;
							}
						}
						else if (identifyTestCase(rsForTestCases.getString("field_type"), rsForTestCases.getString("input_value"),
								rsForTestCases.getString("action")) == "ACTION") {
							logger.debug("testcaseId " + currentTestCaseId + " identified as ACTION");
							try {
								status = chromeTester.actionPageElement(conn, app.getUrl(), app.getLoginName(), app.getLoginPwd(), rsForTestCases.getString("field_name"), rsForTestCases.getString("field_type"), rsForTestCases.getString("read_element"), rsForTestCases.getString("xpath"), rsForTestCases.getString("action"), rsForTestCases.getString("action_url"),rsForTestCases.getString("base_url"), rsForTestCases.getString("need_screenshot"), rsForTestCases.getString("description"), currentSchedulerId, currentTestCaseId, currentTestSequence);
								sleepIfInstructedTo(rsForTestCases.getString("sleeps"));
							}catch (NoSuchElementException nse) {
								logger.error(nse.getMessage());
								updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, com.rsi.utils.RsiTestingHelper.returmTimeStamp(), com.rsi.utils.RsiTestingHelper.returmTimeStamp());
								//nse.printStackTrace();
								continue;
							}catch(InterruptedException ie) {
								logger.error(ie.getMessage());
								updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, com.rsi.utils.RsiTestingHelper.returmTimeStamp(), com.rsi.utils.RsiTestingHelper.returmTimeStamp());
								ie.printStackTrace();
								continue;
							}
						}
						else if(identifyTestCase(rsForTestCases.getString("field_type"), rsForTestCases.getString("input_value"),
								rsForTestCases.getString("action")) == "INPUT") {
							logger.debug("testcaseId " + currentTestCaseId + " identified as INPUT");
							try{
								status = chromeTester.inputPageElement(conn, app.getUrl(), app.getLoginName(), app.getLoginPwd(), rsForTestCases.getString("field_name"), rsForTestCases.getString("field_type"), rsForTestCases.getString("input_value"), rsForTestCases.getString("xpath"), rsForTestCases.getString("base_url"), rsForTestCases.getString("need_screenshot"), rsForTestCases.getString("description"), rsForTestCases.getString("enter_action"), currentSchedulerId, currentTestCaseId, currentTestSequence);
								sleepIfInstructedTo(rsForTestCases.getString("sleeps"));
							}catch(NoSuchElementException nse) {
								logger.error("Error when handling Input type case... " + nse.getMessage());
								updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, com.rsi.utils.RsiTestingHelper.returmTimeStamp(), com.rsi.utils.RsiTestingHelper.returmTimeStamp());
								continue;
							}catch(InterruptedException ie) {
								logger.error(ie.getMessage());
								updateTestCaseWithError(conn, currentTestCaseId, currentSchedulerId, com.rsi.utils.RsiTestingHelper.returmTimeStamp(), com.rsi.utils.RsiTestingHelper.returmTimeStamp());
								ie.printStackTrace();
								continue;
							}
						}
						else if(identifyTestCase(rsForTestCases.getString("field_type"), rsForTestCases.getString("input_value"),
								rsForTestCases.getString("action")) == "CUSTOM") {
							logger.debug("testcaseId " + currentTestCaseId + " identified as CUSTOM");
							String startTime = com.rsi.utils.RsiTestingHelper.returmTimeStamp();
							String endTime = null;
							// TODO now try to instantiate custom application code to execute backend methods that could not be performed from frontend. such as cleanup an object.
							// Now assume that a custom class has been implemented for this application.
							logger.info("Now inside custom code.");
							String commandName = rsForTestCases.getString("field_name").substring(1, rsForTestCases.getString("field_name").length()-1);
							//CommonHealthCore_Dev chcDev = new CommonHealthCore_Dev(app, conn);
							for (CustomCommand command : app.getCustomCommands()) {
								if(commandName.equalsIgnoreCase(command.getCustomCommandName())) {
									logger.debug("Found the command to run " + command.toString());
									try {
										String all_params = "";
										all_params = buildParamString(command.getParams(), rsForTestCases.getString("read_element"));
										Process p = Runtime.getRuntime().exec(command.getCustomCommand() + " " + all_params);
										if (!p.waitFor(1, TimeUnit.MINUTES)) {
											InputStreamReader ssReader = new InputStreamReader(p.getInputStream());
											//InputStreamReader isReader = new InputStreamReader(p.getErrorStream());
											//Creating a BufferedReader object
											BufferedReader reader = new BufferedReader(ssReader);
											StringBuffer sb = new StringBuffer();
											String str;
											while((str = reader.readLine())!= null){
												sb.append(str);
											}
											if (com.rsi.utils.RsiTestingHelper.checkEmpty(sb.toString())) {
												status = "Failure";
											}
											else {
												logger.info("Return from Custom Script [ " +  sb.toString() + " ]");
												status = "Success";
											}
										}else {
											p.destroy(); // consider using destroyForcibly instead
										}
									} catch (IOException ioe) {
										status = "Failure";
									}
									catch(RuntimeException re) {
										status = "Failure";
										logger.error("re.getMessage()");
									} catch (InterruptedException ine) {
										status = "Failure";
										logger.error("ine.getMessage()");
									}
								}

							}
							if(status.equalsIgnoreCase("Failure")) {
								updateTestCaseWithError(conn,currentTestCaseId,currentSchedulerId, startTime, endTime);
							} else {
								updateTestCaseWithSuccess(conn,currentTestCaseId,currentSchedulerId, startTime, endTime);
							}
						}
						logger.debug("Status returned is [ " + status + " ]");
						if(!com.rsi.utils.RsiTestingHelper.checkEmpty(rsForTestCases.getString("new_tab")) && rsForTestCases.getString("new_tab").equalsIgnoreCase("1")) {
							chromeTester.switchToNewTab();
							try {
								TimeUnit.SECONDS.sleep(5);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

					}
				}
				if(status.equalsIgnoreCase("SUCCESS")) {
					updateSchedulerWithComplete(conn, currentSchedulerId, currentSuiteId);
				}
				else {
					updateSchedulerWithError(conn, currentSchedulerId, currentSuiteId, app);
				}
				// send out an email to the recipient of this environment.
				EmailManagementUtility.sendEmail(app, currentSchedulerId, conn);
			}
		} catch (SQLException e) {
			logger.error("Looks like Something bad happened while running the query, most likely referential data does not exist. ");
			logger.error (e.getMessage());
			//e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
		            rs.close();
		        } catch (SQLException sqlEx) { } // ignore

		        rs = null;
			}
			if (stmt != null) {
		        try {
		            stmt.close();
		        } catch (SQLException sqlEx) { } // ignore

		        stmt = null;
		    }
			
			chromeTester.getDriver().quit();
		}
	    
	}

	private static void sleepIfInstructedTo(String sleep) throws InterruptedException {
		//rsForTestCases.getString("sleeps")
		if(!com.rsi.utils.RsiTestingHelper.checkEmpty(sleep)) {
			try {
				int iSleepTimeInSecs = Integer.parseInt(sleep);
				TimeUnit.SECONDS.sleep(iSleepTimeInSecs);
			} catch (NumberFormatException nfe) {
				logger.debug("Could not convert the sleep time to a number sleep time set was [ " + sleep + " ]");
				TimeUnit.SECONDS.sleep(15);
			}
		}

	}

	private static String buildParamString(String[] params, String read_element) {
		ArrayList<String> outputParams = new ArrayList<String>();
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(read_element);
			logger.debug("JSON Object retrieved is " + jsonObject.toString());
			for (String param : params){
				try {
					outputParams.add((String)jsonObject.get(param));
				}catch (NullPointerException npe) {
					npe.printStackTrace();
					logger.error("no record found " + npe.getMessage());
				}

			}
		}catch (JSONException err){
			logger.error( err.toString());
			return "";
		}
		String csv = outputParams.toString().replace("[", "").replace("]", "")
				.replace(", ", " ");
		logger.debug("csv is [" + csv + "]");
		return csv;
	}

	private static void updateSchedulerWithComplete(Connection conn,
			int currentSchedulerId, int currentSuiteId) {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE schedulers SET status = 'Complete' WHERE id = ?");
			pstmt.setInt(1, currentSchedulerId);
			if (pstmt.execute()) {
				logger.info("Updated Scheduler id [" + currentSchedulerId + " ], with Complete");
			}
			else {
				logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Complete Status. Please delete it manually. ");
			}
		} catch (SQLException e) {
			logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Complete Status. Please delete it manually. ");
			e.printStackTrace();
		}finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			pstmt = conn.prepareStatement("INSERT INTO result_suites (rd_id, scheduler_id, test_suite_id) VALUES(?,?,?)");
			pstmt.setInt(1, 1);
			pstmt.setInt(2, currentSchedulerId);
			pstmt.setInt(3, currentSuiteId);
			if (pstmt.execute()) {
				logger.info("Updated Scheduler id [" + currentSchedulerId + " ], with Error");
			}
			else {
				logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Error Status. Please delete it manually. ");
			}
				
		} catch (SQLException e) {
			logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Error Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}

	private static void updateTestCaseWithError(Connection conn,
			int currentTestCaseId, int currentSchedulerId, String startTime, String endTime) {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO result_cases (rd_id,test_case_id,scheduler_id, error_description, created_at, updated_at) VALUES (?, ?,?,?,?,?)");
			pstmt.setInt(1, 2);
			pstmt.setInt(2, currentTestCaseId);
			pstmt.setInt(3, currentSchedulerId);
			pstmt.setString(4, "Custom Command run successfully.");
			pstmt.setString(5, startTime);
			pstmt.setString(6, endTime);
			pstmt.execute();
			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				logger.info("Updated TestCase Result id [" + currentTestCaseId + " ], with Error");
			}
			else {
				logger.error("Could not update the Test Case id in Results [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			}
		} catch (SQLException e) {
			logger.error("Could not update the Test Case with Result for id [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}

	private static void updateTestCaseWithSuccess(Connection conn,
												int currentTestCaseId, int currentSchedulerId, String startTime, String endTime) {
		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("INSERT INTO result_cases (rd_id,test_case_id,scheduler_id, error_description, created_at, updated_at) VALUES (?, ?,?,?,?,?)");
			pstmt.setInt(1, 1);
			pstmt.setInt(2, currentTestCaseId);
			pstmt.setInt(3, currentSchedulerId);
			pstmt.setString(4, "Custom Command run successfully.");
			pstmt.setString(5, startTime);
			pstmt.setString(6, endTime);
			pstmt.execute();
			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				logger.info("Updated TestCase Result id [" + currentTestCaseId + " ], with Error");
			}
			else {
				logger.error("Could not update the Test Case id in Results [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			}

		} catch (SQLException e) {
			logger.error("Could not update the Test Case with Result for id [" + currentTestCaseId + " ] with Error Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	private static boolean updateSchedulerWithRunning(Connection conn, int currentSchedulerId, int currentSuiteId, H2OApplication app) {
		PreparedStatement pstmt = null;
		boolean retVal = true;
		try {
			pstmt = conn.prepareStatement("UPDATE schedulers SET status = 'Running' WHERE id = ?");
			pstmt.setInt(1, currentSchedulerId);

			pstmt.execute();
			logger.info("Updated Scheduler id [" + currentSchedulerId + "], with running");
		} catch (SQLException sqle) {
			logger.error("Could not update the Scheduler id [" + currentSchedulerId + "], with running, this process should exit without going any further");
			logger.error(sqle);
			retVal = false;
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				logger.error("ERROR when updating the scheduler to be in running state...", e);
			}
		}
		return retVal;
	}

	private static void updateSchedulerWithError(Connection conn, int currentSchedulerId, int currentSuiteId, H2OApplication app) {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE schedulers SET status = 'Error' WHERE id = ?");
			pstmt.setInt(1, currentSchedulerId);
			if (pstmt.execute()) {
				logger.info("Updated Scheduler id [" + currentSchedulerId + " ], with Error");
			}
			else {
				logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Error Status. Please delete it manually. ");
			}
				
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Error Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			pstmt = conn.prepareStatement("INSERT INTO result_suites (rd_id, test_suite_id, scheduler_id) VALUES(?,?,?)");
			pstmt.setInt(1, 2);
			pstmt.setInt(2, currentSuiteId);
			pstmt.setInt(3, currentSchedulerId);

			if (pstmt.execute()) {
				logger.info("Updated Scheduler id [" + currentSchedulerId + " ], with Error");
			}
			else {
				logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Error Status. Please delete it manually. ");
			}
				
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Could not update the Scheduler id [" + currentSchedulerId + " ] with Error Status. Please delete it manually. ");
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}

	private static String identifyTestCase(String fieldType, String inputValue,
			String action) {
		if(!com.rsi.utils.RsiTestingHelper.checkEmpty(fieldType) && fieldType.startsWith("[") && fieldType.endsWith("]")) {
			return "CUSTOM";
		}
		if(!com.rsi.utils.RsiTestingHelper.checkEmpty(action) && !action.equalsIgnoreCase("read")) {
			return "ACTION";
		}
		if(!com.rsi.utils.RsiTestingHelper.checkEmpty(action) && action.equalsIgnoreCase("read")) {
			return "INSPECT";
		}

		//string - field_type ----- string2 - input_value ----------- string3 - action
		if (fieldType.equalsIgnoreCase("anchor") || fieldType.equalsIgnoreCase("a") || fieldType.equalsIgnoreCase("span") || fieldType.equalsIgnoreCase("select") || fieldType.equalsIgnoreCase("option")){
			if (!com.rsi.utils.RsiTestingHelper.checkEmpty(action) || !action.equalsIgnoreCase("Read")) {
				if(com.rsi.utils.RsiTestingHelper.checkEmpty(action) && !com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue)) {
					return "INPUT";
				}
				else {
					return "ACTION";
				}
			}
			else {
				return "INSPECT";
			}
		}
		else if (com.rsi.utils.RsiTestingHelper.checkEmpty(action) && fieldType.equalsIgnoreCase("label")){
			return "INSPECT";
		}
		else if(fieldType.equalsIgnoreCase("checkbox") || fieldType.equalsIgnoreCase("radio")) {
			if (!com.rsi.utils.RsiTestingHelper.checkEmpty(action)) {
				return "ACTION";
			}
			else {
				return "INSPECT";
			}
		}
		else if(fieldType.equalsIgnoreCase("button")) {
			if(!com.rsi.utils.RsiTestingHelper.checkEmpty(action)) {
				return "ACTION";
			}
			else {
				return "INSPECT";
			}

		}
		else if (fieldType.equalsIgnoreCase("text") || fieldType.equalsIgnoreCase("textarea") || fieldType.equalsIgnoreCase("search")){
			if(!com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue)){
				return "INPUT";
			}
			else if (com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue) && action == null){
				return "INSPECT";
			}
			else if (com.rsi.utils.RsiTestingHelper.checkEmpty(inputValue) && action != null) {
				return "ACTION";
			}
		}
		
		return "INSPECT";
	}

}
