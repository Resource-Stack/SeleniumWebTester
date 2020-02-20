package com.rsi.selenium.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import com.rsi.dataObject.CustomCommand;
import org.apache.log4j.Logger;

import com.rsi.dataObject.H2OApplication;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public class H2OTesterConnectionFactory {
	final static Logger logger = Logger.getLogger(H2OTesterConnectionFactory.class);
	private static H2OTesterConnectionFactory factoryInstance = getInstance( );
	private static Connection dbInstance = null;
	private static HashMap<Integer,H2OApplication> environments = new HashMap<Integer,H2OApplication>(); 
	   
	/* A private Constructor prevents any other 
	* class from instantiating.
	*/
	private H2OTesterConnectionFactory(){ 
		try {
			logger.debug("Now ready to load the mysql driver");
			Class.forName("com.mysql.jdbc.Driver"); //.newInstance();
			logger.debug("Mysql driver loaded successfully...");
			
			Connection conn = getDatabaseConnection();
			try {
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT id, url, username, password, login_field, password_field,name, action_button FROM environments");
				while (rs.next()) {
					logger.debug("Resultset login field is " + rs.getString("login_field") + " rs id is ");
					H2OApplication app = new H2OApplication();
					app.setUrl(rs.getString("url"));
					app.setLoginName(rs.getString("username"));
					app.setLoginPwd(rs.getString("password"));
					app.setLoginField(rs.getString("login_field"));
					app.setPasswordField(rs.getString("password_field"));
					app.setActionButton(rs.getString("action_button"));
					app.setName(rs.getString("name"));
					app.setAppId(rs.getInt("id"));
					app.setCustomCommands(constructCustomApplication(conn, rs.getInt("id")));
					logger.debug("Now ready to put a value in environments [ " + rs.getInt("id")+ " ], app is [ " + app.toString() + " ]");
					if (environments == null) {
						environments = new HashMap<Integer, H2OApplication>();
					}
					environments.put(new Integer(rs.getInt("id")), app);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
					
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("# of environments is [ " + environments.size() + " ]");
		Iterator it = environments.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        logger.debug("Key is" + pair.getKey() + " = value is" + pair.getValue());
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
		
	}

	private ArrayList<CustomCommand> constructCustomApplication(Connection conn, int id) {
		ArrayList<CustomCommand> commandsForEnvironment = new ArrayList<CustomCommand>();

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT  id, name, command, parameters FROM custom_commands where environment_id = " + id);
			while (rs.next()) {
				logger.debug("Found the record for a custom command " + rs.getInt("id"));
				CustomCommand command = new CustomCommand(rs.getInt("id"), rs.getString("name"), rs.getString("command"), rs.getString("parameters").substring(1,rs.getString("parameters").length()-1).split(","));
				logger.debug("Custom COmmand is " + command.toString() + " now adding it to the environment");
				commandsForEnvironment.add(command);
			}
		}catch (SQLException sqle) {
			// TODO add exception handler logic here.
			sqle.printStackTrace();
		}

		return commandsForEnvironment;
	}

	/* Static 'instance' method */
	public static H2OTesterConnectionFactory getInstance( ) {
		if (factoryInstance == null) {
			try {
				factoryInstance = new  H2OTesterConnectionFactory();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return factoryInstance;
	}
	/* Other methods protected by singleton-ness */
	public Connection getDatabaseConnection( ) {
		if (dbInstance == null) {
			try {
				//dbInstance = DriverManager.getConnection("jdbc:mysql://192.168.56.101/healthix_development?" +
				//        "user=healthix&password=rsi1111");
				ResourceBundle s=ResourceBundle.getBundle("dbconfig");
				dbInstance = DriverManager.getConnection(s.getString("DBURL") + "?user=" + s.getString("DBUSER")+ "&password=" + s.getString("DBPASS"));
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		logger.debug("Now ready to return a Database Connection");
		return dbInstance;
		
	}
	
	@SuppressWarnings("rawtypes")
	public H2OApplication getApplicationEnvironment(int environmentId) {
		H2OApplication appObject = null;
		Connection conn = getDatabaseConnection(); 
		if (environments.isEmpty()) {
			try {
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT id, url, username, password, login_field, password_field,name, action_button FROM environments");
				while (rs.next()) {
					H2OApplication app = new H2OApplication();
					app.setUrl(rs.getString("url"));
					app.setLoginName(rs.getString("username"));
					app.setLoginPwd(rs.getString("password"));
					app.setLoginField(rs.getString("login_field"));
					app.setPasswordField(rs.getString("password_field"));
					app.setActionButton(rs.getString("action_button"));
					app.setName(rs.getString("name"));
					app.setAppId(rs.getInt("id"));
					app.setCustomCommands(constructCustomApplication(conn, environmentId));
					logger.debug("Now ready to put a value in environments [ " + rs.getInt("id")+ " ], app is [ " + app.toString() + " ]");
					if (environments == null) {
						environments = new HashMap<Integer, H2OApplication>();
					}
					environments.put(new Integer(rs.getInt("id")), app);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		logger.debug("Starting to process getApplicationEnvironment... number of environments are [ " + environments.size() + " ]");
		Iterator it = environments.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        if (((Integer)pair.getKey()).intValue() == environmentId) {
	        	logger.debug("Found it - Key is" + pair.getKey() + " = value is" + pair.getValue());
	        	appObject = (H2OApplication)pair.getValue();
	        }
	        else {
	        	logger.debug("I wonder why - Key is" + pair.getKey() + " = value is" + pair.getValue());
	        }
	        
	        
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
		if (appObject != null) {
			logger.debug("H2OApplication object returned by Hash is [ " + appObject.toString() + " ]");
		}
		
		logger.debug("Ending process getApplicationEnvironment...");
		return appObject;
		
	}

}
