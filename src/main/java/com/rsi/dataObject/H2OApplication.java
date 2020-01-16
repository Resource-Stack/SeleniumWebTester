package com.rsi.dataObject;

public class H2OApplication {
	private String url;
	private String loginField;
	private String passwordField;
	private String actionButton;
	private String loginName;
	private String loginPwd;
	private String name;
	private Integer appId;
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "URL: " + url + " loginName:" + loginName + " loginPwd:" + loginPwd + " loginField:" + loginField + " passwordField:" + passwordField + " actionButton:" + actionButton +" name:" + name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getLoginField() {
		return loginField;
	}
	public void setLoginField(String loginField) {
		this.loginField = loginField;
	}
	public String getPasswordField() {
		return passwordField;
	}
	public void setPasswordField(String passwordField) {
		this.passwordField = passwordField;
	}
	public String getActionButton() {
		return actionButton;
	}
	public void setActionButton(String actionButton) {
		this.actionButton = actionButton;
	}
	public String getLoginName() {
		return loginName;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	public String getLoginPwd() {
		return loginPwd;
	}
	public void setLoginPwd(String loginPwd) {
		this.loginPwd = loginPwd;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getAppId() {
		return appId;
	}
	public void setAppId(Integer appId) {
		this.appId = appId;
	}

}
