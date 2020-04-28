package com.rsi.utils;

import com.rsi.dataObject.H2OApplication;

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
        H2OApplication app = new H2OApplication();
        Connection conn = null;
        EmailManagementUtility.sendEmail(app, 1, conn);
    }
}