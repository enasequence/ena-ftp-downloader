package uk.ac.ebi.ena.ftp.utils;

import java.text.DecimalFormat;

/**
 * Created by suranj on 31/05/2016.
 */
public class Utils {

    public static String getHumanReadableSize(long bytes) { // expects bytes
        String result = "";
        DecimalFormat df = new DecimalFormat("#,##0.00");

        double inFormat = 0;
        if (bytes < 1024) {
            result = bytes + " Bytes";
        } else if (bytes < Math.pow(1024, 2)) {
            inFormat = (bytes/Math.pow(1024, 1));
            //result = new Double(df.format(inFormat)).doubleValue() + " KB";
            result = df.format(inFormat) + " KB";
        } else if (bytes < Math.pow(1024, 3)) {
            inFormat = (bytes/Math.pow(1024, 2));
            //result = new Double(df.format(inFormat)).doubleValue() + " MB";
            result = df.format(inFormat) + " MB";
        } else if (bytes < Math.pow(1024, 4)) {
            inFormat = (bytes/Math.pow(1024, 3));
            //result = new Double(df.format(inFormat)).doubleValue() + " GB";
            result = df.format(inFormat) + " GB";
        } else if (bytes < Math.pow(1024, 5)) {
            inFormat = (bytes/Math.pow(1024, 4));
            //result = new Double(df.format(inFormat)).doubleValue() + " TB";
            result = df.format(inFormat) + " TB";
        } else if (bytes < Math.pow(1024, 6)) {
            inFormat = (bytes/Math.pow(1024, 5));
            //result = new Double(df.format(inFormat)).doubleValue() + " PB";
            result = df.format(inFormat) + " PB";
        }

        return result;
    }
}
