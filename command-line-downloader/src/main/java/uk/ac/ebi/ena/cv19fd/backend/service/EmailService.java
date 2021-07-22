package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.services.MenuService;
import uk.ac.ebi.ena.cv19fd.app.utils.FileUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Service
@Slf4j
public class EmailService {

    private static final String subject = "Covid-19 %s %s file download completed";
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

    public static String constructEmailMessageForFastqSubmitted(long successfulDownloads, long failedDownloads,
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

    public static String constructSuccessEmailMsgForOtherDownloads(String fileDownloaderPath) {
        String message = "File download successfully completed to " + fileDownloaderPath + ".";
        return encode(message);
    }


    public static String constructFailureEmailMsgForOtherDownloads(String scriptName) {
        String message = "File download failed due to possible network issues. Please re-run the same script" +
                "(" + scriptName + ")" + "to re-attempt to download those files";

        return encode(message);
    }

    public static String constructSubject(DataTypeEnum dataType, DownloadFormatEnum format) {
        return encode(String.format(subject, dataType, format));
    }

    public static String constructEmailName() {
        return encode("For any issues/support please contact us using " + supportAPILink + "\n" +
                "European Nucleotide Archive: Data Coordination & Presentation\n" +
                "EMBL-EBI");
    }

    public void sendEmailForFastqSubmitted(String recipientEmailId, long successfulDownloadsCount,
                                           long failedDownloadsCount, String scriptFileName, DataTypeEnum dataType,
                                           DownloadFormatEnum format, String downloadLocation) {
        if (StringUtils.isNotEmpty(recipientEmailId) && !MenuService.NONE.equals(recipientEmailId)) {
            String emailMessage = EmailService.constructEmailMessageForFastqSubmitted(successfulDownloadsCount,
                    failedDownloadsCount, scriptFileName, downloadLocation);
            String subject = EmailService.constructSubject(dataType, format);
            String name = EmailService.constructEmailName();
            enaPortalService.sendEmail(recipientEmailId, emailMessage, subject, name);
        }
    }

    public void sendEmailForOtherFormats(String recipientEmailId, DomainEnum domain, DataTypeEnum dataType,
                                         DownloadFormatEnum format,
                                         String fileDownloaderPath, boolean isSuccess, List<String> accessions) {
        if (StringUtils.isEmpty(recipientEmailId) || MenuService.NONE.equals(recipientEmailId)) {
            return;
        }
        String emailMessage = null;
        String scriptFileName = FileUtils.getScriptPath(domain, dataType, format, accessions);
        if (isSuccess) {
            emailMessage = EmailService.constructSuccessEmailMsgForOtherDownloads(fileDownloaderPath);
        } else {
            emailMessage = EmailService.constructFailureEmailMsgForOtherDownloads(scriptFileName);
        }

        String subject = EmailService.constructSubject(dataType, format);
        String name = EmailService.constructEmailName();
        enaPortalService.sendEmail(recipientEmailId, emailMessage, subject, name);
    }


}
