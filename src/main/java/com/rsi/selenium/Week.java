package com.rsi.selenium;

class Wednesday {
    private int dayNum;
    private String dayName;

    // Default Constructor
    public Wednesday() {
        dayNum = 3;
        dayName = "Hump Day";
    }

    //overloaded Constructor
    public Wednesday(int dn, String dNm) {
        dayNum = dn;
        dayName = dNm;
    }

    public int getDayNum() {
        return dayNum;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayNum(int dayNum) {
        this.dayNum = dayNum;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }


} // end of class Wednesday

class Week {
    public static void main(String[] args) {
        Wednesday w = new Wednesday();

        Wednesday wSpanish = new Wednesday(3, "Spanish for wednesday");

        Wednesday wConventionBreaker = new Wednesday();
        wConventionBreaker.setDayNum(8);
        wConventionBreaker.setDayName("Sameer");
    }
}