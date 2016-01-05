package com.yli.derg.utils;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by liyc on 12/23/15.
 * useful utils
 */
public class Util {
    public static final Logger LOGGER = Logger.getLogger("UnuglifyDEX");
    public static final String UNKNOWN = "<unknown>";

    public static String getTimeString() {
        long timeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmmss");
        Date date = new Date(timeMillis);
        return sdf.format(date);
    }

    public static void logException(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Util.LOGGER.warning(sw.toString());
    }

    public static float safeDivide(int obfuscated, int total) {
        if (total <= 0) return 1;
        return (float) obfuscated / total;
    }

}
