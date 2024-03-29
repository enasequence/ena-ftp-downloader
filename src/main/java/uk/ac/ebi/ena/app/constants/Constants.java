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

package uk.ac.ebi.ena.app.constants;

public class Constants {
    public static final String enterMessage = " enter ";
    public static final String forMessage = "For ";
    public static final String toMessage = "To ";
    public static final String backMessage = "To go back enter b";

    public static final String SEMICOLON = ";";
    public static final int CHUNK_SIZE = 1000;
    public static final int EXECUTOR_THREAD_COUNT = 3;
    public static final String FTP = "ftp://";
    public static final int TOTAL_RETRIES = 5;

    public static final String binFolder = "bin";
    public static final String etcFolder = "etc";
    public static final String ascpFileName = "ascp";
    public static final String asperaWebFile = "asperaweb_id_dsa.openssh";


    public static final String FTP_SRA_SERVER = "ftp.sra.ebi.ac.uk";

    public static final String DCC_PRIVATE_FTP_FILE_PATH = "dcc-private.ebi.ac.uk";

    public static final String PORTAL_API_EP = "https://www.ebi.ac.uk/ena/portal/api";

    public static final String URLENCODED = "application/x-www-form-urlencoded";
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";

    public static final String CLIENT_PARAM = "&client=ena_file_downloader";


}
