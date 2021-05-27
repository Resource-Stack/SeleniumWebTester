package com.rsi.selenium.factory;

import com.rsi.dataObject.CustomCommand;
import com.rsi.dataObject.H2OApplication;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

public class H2OTesterConnectionFactory {
	final static Logger logger = Logger.getLogger(H2OTesterConnectionFactory.class);
	private static H2OTesterConnectionFactory factoryInstance = getInstance();
	private static Connection dbInstance = null;

	/*
	 * A private Constructor prevents any other class from instantiating.
	 */
	private H2OTesterConnectionFactory() {
		try {
			logger.debug("Now ready to load the mysql driver");
			Class.forName("com.mysql.jdbc.Driver"); // .newInstance();
			logger.debug("Mysql driver loaded successfully...");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private ArrayList<CustomCommand> constructCustomApplication(Connection conn, int id) {
		ArrayList<CustomCommand> commandsForEnvironment = new ArrayList<CustomCommand>();

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(
					"SELECT  id, name, command, parameters FROM custom_commands where environment_id = " + id);
			while (rs.next()) {
				logger.debug("Found the record for a custom command " + rs.getInt("id"));
				String[] parameters = new String[] {};

				String parms = rs.getString("parameters");
				if (parms != null && parms.trim().length() > 0) {
					parameters = parms.substring(1, parms.length() - 1).split(",");
				}
				CustomCommand command = new CustomCommand(rs.getInt("id"), rs.getString("name"),
						rs.getString("command"), parameters);
				commandsForEnvironment.add(command);
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

		return commandsForEnvironment;
	}

	/* Static 'instance' method */
	public static H2OTesterConnectionFactory getInstance() {
		if (factoryInstance == null) {
			try {
				factoryInstance = new H2OTesterConnectionFactory();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return factoryInstance;
	}

	/* Other methods protected by singleton-ness */
	public Connection getDatabaseConnection() {
		if (dbInstance == null) {
			try {
				ResourceBundle rb = ResourceBundle.getBundle("dbconfig");

				String connectionString = rb.getString("DBURL")
						+ "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false"
						+ "&user=" + rb.getString("DBUSER") + "&password=" + rb.getString("DBPASS");

				dbInstance = DriverManager.getConnection(connectionString);

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		logger.debug("Now ready to return a Database Connection");
		return dbInstance;

	}

	@SuppressWarnings("rawtypes")
	public H2OApplication getApplicationEnvironment(int environmentId) {
		Connection conn = getDatabaseConnection();
		H2OApplication app = new H2OApplication();

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(
					"SELECT id, url, username, password, login_field, password_field,name, action_button, user_emails, login_required FROM environments where id='"
							+ environmentId + "' LIMIT 1");
			while (rs.next()) {
				app.setUrl(rs.getString("url"));
				app.setLoginName(rs.getString("username"));
				app.setLoginPwd(rs.getString("password"));
				app.setLoginField(rs.getString("login_field"));
				app.setLoginRequired(rs.getBoolean("login_required"));
				app.setPasswordField(rs.getString("password_field"));
				app.setActionButton(rs.getString("action_button"));
				app.setName(rs.getString("name"));
				app.setAppId(rs.getInt("id"));
				app.setUserEmails(rs.getString("user_emails"));
				app.setCustomCommands(constructCustomApplication(conn, environmentId));
				logger.debug("Now ready to put a value in environments [ " + rs.getInt("id") + " ], app is [ "
						+ app.toString() + " ]");
			}

		} catch (Exception e) {
		}

		return app;
	}

}
