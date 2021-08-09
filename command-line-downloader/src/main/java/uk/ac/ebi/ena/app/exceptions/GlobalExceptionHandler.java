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

package uk.ac.ebi.ena.app.exceptions;

import uk.ac.ebi.ena.app.utils.FileUtils;

import java.util.Arrays;

public final class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        FileUtils.writeExceptionToFile("File Reading Exception Trace: " + Arrays.toString(e.getStackTrace()));
        System.out.print("Something went wrong! Please report to the EMBL-EBI ENA helpdesk at https://www.ebi.ac.uk/ena/browser/support . Please provide the contents of the app.log file.");
    }
}
