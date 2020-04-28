package com.rsi.utils;

import com.rsi.dataObject.H2OApplication;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.ResourceBundle;

public class EmailManagementUtility {
    private static String host = "smtp.office365.com";
    private static String username = "technicalsupport@resourcestack.com";
    private static String password = "ZAQ!2wsxZAQ!2wsx";
    // Get system properties
    private static Properties properties = System.getProperties();
    // Get the default Session object.
    private static Session session = null;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

    // Get the default Session object.
    public static void sendEmail(H2OApplication app, int currentSchedulerId, Connection conn) {
        ResourceBundle s=ResourceBundle.getBundle("email");
        username = s.getString("USERNAME");
        password = s.getString("PASSWORD");
        properties.setProperty("mail.smtp.host", s.getString("HOST"));
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.user", s.getString("USERNAME"));
        properties.setProperty("mail.smtp.password", s.getString("PASSWORD"));
        properties.setProperty("mail.smtp.port", s.getString("PORT"));

        session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
        // Create a default MimeMessage object.
        MimeMessage message = new MimeMessage(session);
        // Set From: header field of the header.
        try {
            message.setFrom(new InternetAddress("technicalsupport@resourcestack.com"));


            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("sameerrsi@gmail.com"));

            String suiteName = getSchedulerSuiteName(conn, currentSchedulerId);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String currentTimeVal = sdf.format(timestamp);
            // Set Subject: header field
            message.setSubject("RSI Test Scheduler completed for " + suiteName + " on " + currentTimeVal + ".");

            // Send the actual HTML message, as big as you like
            message.setContent("<h1>Testing completed for " + suiteName + " at " + currentTimeVal + "</h1><br/> You can view the report at " + s.getString("SERVER_URL") + "/list_all_reports?id=" + currentSchedulerId, "text/html");

            // Send message
            session.getTransport().send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private static String getSchedulerSuiteName(Connection conn, int currentSchedulerId) throws SQLException{
        String suiteName = "unknown";
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select ts.name name from test_suites ts, schedulers s where s.test_suite_id = ts.id and s.id =?");
            stmt.setInt(1, currentSchedulerId);
            stmt.execute();
            ResultSet rs = stmt.getResultSet();

            while(rs.next()) {
                suiteName = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            stmt.close();
        }

        return suiteName;
    }

}
