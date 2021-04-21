package com.rsi.dataObject;

import java.util.ArrayList;

import com.rsi.selenium.RsiChromeTester;
import com.rsi.utils.RsiTestingHelper;

public class TestCase {
    private Integer id;
    private String field_name;
    private String field_type;
    private String read_element;
    private String input_value;
    private String action;
    private String action_url;
    private String base_url;
    private String xpath;
    private String sleeps;
    private String new_tab;
    private String description;
    private String need_screenshot;
    private String enter_action;
    private Integer custom_command_id;
    private Integer sequence;
    private Boolean javascript_conditional_enabled;
    private String javascript_conditional;
    private ArrayList<Integer> accepted_case_ids;
    private ArrayList<Integer> rejected_case_ids;

    public ArrayList<Integer> getAcceptedCaseIds() {
        return accepted_case_ids;
    }

    public void setAcceptedCaseIds(String value) {
        String[] splits = value.replaceAll("\\[|\\]| ", "").split(",", -1);
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (String fav : splits) {
            if (!RsiTestingHelper.checkEmpty(fav)) {
                arrayList.add(Integer.parseInt(fav.trim()));
            }
        }
        accepted_case_ids = arrayList;
    }

    public ArrayList<Integer> getRejectedCaseIds() {
        return rejected_case_ids;
    }

    public void setRejectedCaseIds(String value) {
        String[] splits = value.replaceAll("\\[|\\]| ", "").split(",");
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (String fav : splits) {
            if (!RsiTestingHelper.checkEmpty(fav)) {
                arrayList.add(Integer.parseInt(fav.trim()));
            }
        }
        rejected_case_ids = arrayList;
    }

    public Boolean getJavascriptConditionalEnabled() {
        return javascript_conditional_enabled;
    }

    public void setJavascriptConditionalEnabled(Boolean value) {
        javascript_conditional_enabled = value;
    }

    public String getJavascriptConditional() {
        return javascript_conditional;
    }

    public void setJavascriptConditional(String value) {
        javascript_conditional = value;
    }

    public Integer getCustomCommandID() {
        return custom_command_id;
    }

    public void setCustomCommandID(Integer value) {
        custom_command_id = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNeedScreenshot() {
        return need_screenshot;
    }

    public void setNeedScreenshot(String need_screenshot) {
        this.need_screenshot = need_screenshot;
    }

    public String getNewTab() {
        return new_tab;
    }

    public void setNewTab(String new_tab) {
        this.new_tab = new_tab;
    }

    public String getEnterAction() {
        return enter_action;
    }

    public void setEnterAction(String enter_action) {
        this.enter_action = enter_action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getSleeps() {
        return sleeps;
    }

    public void setSleeps(String sleeps) {
        this.sleeps = sleeps;
    }

    public String getActionUrl() {
        return action_url;
    }

    public void setActionUrl(String action_url) {
        this.action_url = action_url;
    }

    public String getXPath() {
        return xpath;
    }

    public void setXPath(String xpath) {
        this.xpath = xpath;
    }

    public String getFieldName() {
        return field_name;
    }

    public void setfieldName(String field_name) {
        this.field_name = field_name;
    }

    public String getfieldType() {
        return field_type;
    }

    public void setfieldType(String field_type) {
        this.field_type = field_type;
    }

    public String getReadElement() {
        return read_element;
    }

    public void setReadElement(String read_element) {
        this.read_element = read_element;
    }

    public String getInputValue() {
        return input_value;
    }

    public void setInputValue(String input_value) {
        this.input_value = input_value;
    }

    public String getBaseUrl() {
        return base_url;
    }

    public void setBaseUrl(String base_url) {
        this.base_url = base_url;
    }

}
