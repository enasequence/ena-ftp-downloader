package uk.ac.ebi.ena.cv19fd.app.utils;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Created by raheela on 20/04/2021.
 */
@Component
@NoArgsConstructor
public class ScannerUtils {
    public Scanner scanner = null;

    private Scanner getScannerObject() {
        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        return scanner;
    }

    public int getNextInt() {
        String input = getScannerObject().nextLine();
        if (isInputGoBack(input)) {
            return -1;
        }
        return CommonUtils.stringToInput(input);
    }

    private boolean isInputGoBack(String input) {
        return "b".equalsIgnoreCase(input);
    }

    public String getNextString() {
        return getScannerObject().nextLine();
    }

}
