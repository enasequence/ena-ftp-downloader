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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BackendServiceImpl implements BackendService {

    private final AccessionDetailsService accessionDetailsService;

    public BackendServiceImpl(AccessionDetailsService accessionDetailsService) {
        this.accessionDetailsService = accessionDetailsService;
    }

    @Override
    public void startDownload(DownloadFormatEnum format, String location, Map<String, List<String>> accessionDetailsMap,
                              ProtocolEnum protocol, String asperaConnectLocation, String emailId) {

        log.info("Starting download for format:{} at download location:{},protocol:{}, asperaLoc:{}, emailId:{}",
                format, location, protocol, asperaConnectLocation, emailId);
        long failedCount = 0;
        do {
            failedCount = accessionDetailsService.fetchAccessionAndDownload(format, location, accessionDetailsMap, protocol
                    , asperaConnectLocation, emailId);
            if (failedCount > 0) {
                System.out.println("Automatically retrying failed downloads...");
            }
        } while (failedCount > 0);

    }
}
