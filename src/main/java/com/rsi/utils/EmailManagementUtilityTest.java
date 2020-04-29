package com.rsi.utils;

import com.rsi.dataObject.H2OApplication;
import com.rsi.selenium.factory.H2OTesterConnectionFactory;

import java.sql.Connection;

class EmailManagementUtilityTest {

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void sendEmail() {
        H2OApplication app = H2OTesterConnectionFactory.getInstance().getApplicationEnvironment(4);
        Connection conn = H2OTesterConnectionFactory.getInstance().getDatabaseConnection();

        EmailManagementUtility.sendEmail(app, 1, conn);
    }
}