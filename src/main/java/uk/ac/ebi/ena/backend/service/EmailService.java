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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.services.MenuService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Service
@Slf4j
public class EmailService {

    private static final String subject = "Ena %s %s file download completed";
    private static final String supportAPILink = "https://www.ebi.ac.uk/ena/browser/support";

    private final EnaPortalService enaPortalService;

    public EmailService(EnaPortalService enaPortalService) {
        this.enaPortalService = enaPortalService;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            log.error("UnsupportedEncodingException encountered while trying to encode {}", value, uee);
        }
        return null;
    }

    private static String constructEmailMessageForFastqSubmitted(long successfulDownloads, long failedDownloads,
                                                                 String scriptName, String downloadLocation) {
        String message =
                "Location:" + downloadLocation +
                        "\nNumber of successful downloads:" + successfulDownloads
                        + "\nNumber of failed downloads:" + failedDownloads;
        if (failedDownloads > 0) {
            message = message + "\n\nSome files failed to download due to possible network issues. Please re-run the " +
                    "same script (" + scriptName + ") to re-attempt to download those files";
        }
        return encode(message);

    }

    private static String constructSubject(String accesionType, DownloadFormatEnum format) {
        return encode(String.format(subject, accesionType, format));
    }

    private static String constructEmailName() {
        return encode("For any issues/support please contact us using " + supportAPILink + "\n" +
                "European Nucleotide Archive: Data Coordination & Presentation\n" +
                "EMBL-EBI");
    }

    public void sendEmailForFastqSubmitted(String recipientEmailId, long successfulDownloadsCount,
                                           long failedDownloadsCount, String scriptFileName, String accessionType,
                                           DownloadFormatEnum format, String downloadLocation) {
        if (StringUtils.isNotEmpty(recipientEmailId) && !MenuService.NONE.equals(recipientEmailId)) {
            String emailMessage = EmailService.constructEmailMessageForFastqSubmitted(successfulDownloadsCount,
                    failedDownloadsCount, scriptFileName, downloadLocation);
            String subject = EmailService.constructSubject(accessionType, format);
            String name = EmailService.constructEmailName();
            enaPortalService.sendEmail(recipientEmailId, emailMessage, subject, name);
        }
    }


}
