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
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.app.utils.FileUtils;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.service.AccessionDetailsService;
import uk.ac.ebi.ena.backend.service.BackendServiceImpl;
import uk.ac.ebi.ena.backend.service.EmailService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BackendServiceImplTest {

    @Mock
    EmailService emailService;

    @Mock
    AccessionDetailsService accessionDetailsService;

    @InjectMocks
    BackendServiceImpl backendService;

    @Test
    public void startDownloadTest() {
        //ARRANGE
        String accessionList = "SRX6415696,SRX2000905,SRX6415695";
        DownloadFormatEnum format = DownloadFormatEnum.READS_FASTQ;
        String location = System.getProperty("user.home");
        List<FileDetail> fileDetailList = createFileDetailFtp();

        DownloadJob accessionDetailsMap = CommonUtils.processAccessions(Arrays.asList(accessionList.split(",")));
        ProtocolEnum protocol = ProtocolEnum.FTP;
        String asperaConnectLocation = null;
        String emailId = "datasubs@ebi.ac.uk";
        Mockito.when(accessionDetailsService.fetchFileDetails(format, accessionDetailsMap, protocol))
                .thenReturn(Collections.singletonList(fileDetailList));
        Mockito.doNothing().when(emailService).sendEmailForFastqSubmitted(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()
                , Mockito.anyString(), Mockito.anyString(), Mockito.any(DownloadFormatEnum.class), Mockito.anyString());
        //ACT
        backendService.startDownload(format, location, accessionDetailsMap, protocol, asperaConnectLocation, emailId);
        //ASSERT
        verify(accessionDetailsService, times(1)).doDownload(format, location, accessionDetailsMap,
                Collections.singletonList(fileDetailList), protocol, asperaConnectLocation);

        verify(emailService, times(1)).sendEmailForFastqSubmitted(emailId, 3, 0,
                FileUtils.getScriptPath(accessionDetailsMap, format), accessionDetailsMap.getAccessionField(), format, location);

    }

    private List<FileDetail> createFileDetailFtp() {
        List<FileDetail> fileDetailList = new ArrayList<>();
        FileDetail fileDetail1 = new FileDetail("SRX2000905", "SRR4000583", "ftp.sra.ebi.ac.uk/vol1/fastq/SRR400/003/SRR4000583/SRR4000583.fastq.gz",
                1174738707L, "a991ce890047ffca760c6de2617b5fec", true);
        FileDetail fileDetail2 = new FileDetail("SRX6415696", "SRR9654360", "ftp.sra.ebi.ac.uk/vol1/fastq/SRR965/000/SRR9654360/SRR9654360.fastq.gz",
                14139836L, "f3611f35a977b8b82a7adcf0a28c397d", true);
        FileDetail fileDetail3 = new FileDetail("SRX6415695", "SRR9654361", "ftp.sra.ebi.ac.uk/vol1/fastq/SRR965/001/SRR9654361/SRR9654361.fastq.gz",
                15541843L, "1236b79cd93a63289841765aabacb880", true);
        fileDetailList.add(fileDetail1);
        fileDetailList.add(fileDetail2);
        fileDetailList.add(fileDetail3);

        return fileDetailList;
    }

}
