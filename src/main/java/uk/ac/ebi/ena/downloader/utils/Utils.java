/*
 * Copyright (c) 2017  EMBL-EBI.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.ena.downloader.utils;

import java.text.DecimalFormat;

/**
 * Created by suranj on 31/05/2016.
 */
public class Utils {

    public static final String[] UNITS = new String[] {"Bytes", "KB", "MB", "GB", "TB", "PB"};
    public static DecimalFormat df = new DecimalFormat("#,##0.00");

    public static String getHumanReadableSize(long bytes) { // expects bytes
        String result = "";

        double inFormat = 0;
        if (bytes < 1024) {
            result = bytes + " Bytes";
        } else if (bytes < Math.pow(1024, 2)) {
            inFormat = (bytes / Math.pow(1024, 1));
            result = df.format(inFormat) + " KB";
        } else if (bytes < Math.pow(1024, 3)) {
            inFormat = (bytes / Math.pow(1024, 2));
            result = df.format(inFormat) + " MB";
        } else if (bytes < Math.pow(1024, 4)) {
            inFormat = (bytes / Math.pow(1024, 3));
            result = df.format(inFormat) + " GB";
        } else if (bytes < Math.pow(1024, 5)) {
            inFormat = (bytes / Math.pow(1024, 4));
            result = df.format(inFormat) + " TB";
        } else if (bytes < Math.pow(1024, 6)) {
            inFormat = (bytes / Math.pow(1024, 5));
            result = df.format(inFormat) + " PB";
        }

        return result;
    }
}
