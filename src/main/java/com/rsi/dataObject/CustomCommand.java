package com.rsi.dataObject;

import java.util.Arrays;

public class CustomCommand {
    int id;
    private String customCommand;
    private String customCommandName;
    private String[] params;

    public CustomCommand() {
        id = 0;
        customCommand = "";
        customCommandName = "";
    }
    public CustomCommand(int ident, String name, String command) {
        id = ident;
        customCommand = command;
        customCommandName = name;
    }
    public CustomCommand(int ident, String name, String command, String[] paramNames) {
        id = ident;
        customCommand = command;
        customCommandName = name;
        params = paramNames;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomCommand() {
        return customCommand;
    }

    public void setCustomCommand(String customCommand) {
        this.customCommand = customCommand;
    }

    public String getCustomCommandName() {
        return customCommandName;
    }

    public void setCustomCommandName(String customCommandName) {
        this.customCommandName = customCommandName;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "CustomCommand{" +
                "customCommand='" + customCommand + '\'' +
                ", customCommandName='" + customCommandName + '\'' +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
