package com.rsi.dataObject;

public class TestResult {
    private String status;
    private String description;
    private Integer nextCaseId = -1;

    public Integer getNextCaseId() {
        return nextCaseId;
    }

    public void setNextCaseId(Integer caseId) {
        nextCaseId = caseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
