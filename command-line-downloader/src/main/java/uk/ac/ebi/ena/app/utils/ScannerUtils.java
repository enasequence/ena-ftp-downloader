/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.app.utils;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Scanner;

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