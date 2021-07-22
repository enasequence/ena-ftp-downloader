package uk.ac.ebi.ena.cv19fd.backend.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BackendServiceImplTest {

    @Mock
    AccessionDetailsService accessionDetailsService;

    @Mock
    DownloadLocationValidatorService downloadLocationValidatorService;

    @InjectMocks
    BackendServiceImpl backendService;

    @Test
    public void testWhenDownloadLocationValid() {
        //ARRANGE
        Mockito.when(downloadLocationValidatorService.validateDownloadLocation("suman\\Documents")).thenReturn(true);
        //ACT AND ASSERT
        Assertions.assertTrue(backendService.isDownloadLocationValid("suman\\Documents"));
    }

    @Test
    public void testWhenDownloadLocationNotValid() {
        //ARRANGE
        Mockito.when(downloadLocationValidatorService.validateDownloadLocation("suman\\Documents")).thenReturn(false);
        //ACT AND ASSERT
        Assertions.assertFalse(backendService.isDownloadLocationValid("suman\\Documents"));
    }

    @Test
    public void testWhenDownloadSuccess() throws Exception {
        //ARRANGE
        String downloadLoc = "C:\\Users";
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        ProtocolEnum protocol = ProtocolEnum.FTP;
        String emailId = "datasubs@ebi.ac.uk";
        //ACT
        backendService.startDownload(downloadLoc, domain, dataType, format, emailId, new ArrayList<>(), protocol, null);
        //ASSERT
        Mockito.verify(accessionDetailsService, Mockito.times(1)).fetchAccessionAndDownload(domain, dataType, format,
                downloadLoc, emailId, new ArrayList<>()
                , protocol, null);
    }

    @Test
    public void testWhenDownloadSuccessWithAccessionList() throws Exception {
        //ARRANGE
        String downloadLoc = "C:\\Users";
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        ProtocolEnum protocol = ProtocolEnum.FTP;

        String emailId = "datasubs@ebi.ac.uk";
        List<String> accessions = Arrays.asList("MN908947", "LR991698");
        //ACT
        backendService.startDownload(downloadLoc, domain, dataType, format, emailId, accessions, protocol, null);
        //ASSERT
        Mockito.verify(accessionDetailsService, Mockito.times(1)).fetchAccessionAndDownload(domain, dataType, format,
                downloadLoc, emailId, accessions
                , protocol, null);
    }
}
