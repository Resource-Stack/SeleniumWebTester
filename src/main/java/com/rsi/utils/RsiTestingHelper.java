package com.rsi.utils;

public class RsiTestingHelper {
    public static boolean checkEmpty(String s){
        boolean retStatus = false;
        try {
            if(s == null || s.trim().length() == 0) {
                retStatus = true;
            }
            else {
                retStatus = false;
            }
        } catch (NullPointerException npe) {
            return true;
        }


        return retStatus;
    }

    public static String returmTimeStamp() {
        java.util.Date dt = new java.util.Date();

        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String currentTime = sdf.format(dt);
        return currentTime;
    }

}
