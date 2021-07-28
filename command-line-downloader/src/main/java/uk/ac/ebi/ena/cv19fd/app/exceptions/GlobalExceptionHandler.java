package uk.ac.ebi.ena.cv19fd.app.exceptions;

import uk.ac.ebi.ena.cv19fd.app.utils.FileUtils;

import java.util.Arrays;

public final class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        FileUtils.writeExceptionToFile("File Reading Exception Trace: " + Arrays.toString(e.getStackTrace()));
        System.out.print("Something went wrong! Please report to the EMBL-EBI ENA helpdesk at https://www.ebi.ac.uk/ena/browser/support . Please provide the contents of the app.log file.");
    }
}
