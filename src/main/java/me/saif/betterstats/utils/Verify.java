package me.saif.betterstats.utils;

public class Verify {

    private Verify(){}

    public static boolean isNotNull(Object... args){
        if (args == null)
            return false;
        for (Object object : args)
            if (object == null)
                return false;
        return true;
    }

    public static boolean isAnInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isANumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
