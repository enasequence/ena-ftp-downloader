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

package uk.ac.ebi.ena.backend.service;

import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.dto.DownloadJob;

/**
 * Class that will expose methods to download accessions based on the inputs provided by the user
 * This will be used by the command line tool to start the download
 */
public interface BackendService {

    /**
     * @param format                The format provided by the user
     * @param location              The download location
     * @param downloadJob   The accessionDetails map
     * @param emailId               The recipient email Id
     * @param protocol              The protocol for download provided by the user
     * @param asperaConnectLocation The location of aspera connect folder if {@link ProtocolEnum} is ASPERA
     * @param emailId               The emailId at which mail will be sent once downloads are completed
     */

    void startDownload(DownloadFormatEnum format, String location, DownloadJob downloadJob,
                       ProtocolEnum protocol, String asperaConnectLocation, String emailId, AuthenticationDetail authenticationDetail);
}
