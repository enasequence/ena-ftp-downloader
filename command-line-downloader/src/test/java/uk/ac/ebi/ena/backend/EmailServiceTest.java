package uk.ac.ebi.ena.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.backend.service.EmailService;
import uk.ac.ebi.ena.backend.service.EnaPortalService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    @Mock
    EnaPortalService enaPortalService;

    @InjectMocks
    EmailService emailService;

    @Test
    public void testSendEmailForFastqSubmitted() {
        //ARRANGE
        String recipientEmailId = "datasubs@ebi.ac.uk";
        long successfulDownloadsCount = 10;
        long failedDownloadsCount = 1;
        String scriptFileName = "C:\\Users\\Documents\\download_ANALYSIS-ANALYSIS_SUBMITTED.bat";
        String accessionType = "Experiment";
        DownloadFormatEnum format = DownloadFormatEnum.READS_FASTQ;
        String downloadLocation = "C:\\Users";
        //ACT
        emailService.sendEmailForFastqSubmitted(recipientEmailId, successfulDownloadsCount, failedDownloadsCount,
                scriptFileName, accessionType, format, downloadLocation);
        //ASSERT
        verify(enaPortalService, times(1)).sendEmail(Mockito.anyString(), Mockito.anyString()
                , Mockito.anyString(), Mockito.anyString());


    }
}
