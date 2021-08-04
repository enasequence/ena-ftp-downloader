package uk.ac.ebi.ena.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.backend.service.AccessionDetailsService;
import uk.ac.ebi.ena.backend.service.BackendServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BackendServiceImplTest {

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
        ;
        Map<String, List<String>> accessionDetailsMap = CommonUtils.getAccessionDetails(Arrays.asList(accessionList.split(",")));
        ProtocolEnum protocol = ProtocolEnum.FTP;
        String asperaConnectLocation = null;
        String emailId = "datasubs@ebi.ac.uk";
        //ACT
        backendService.startDownload(format, location, accessionDetailsMap, protocol, asperaConnectLocation, emailId);
        //ASSERT
        verify(accessionDetailsService, times(1)).fetchAccessionAndDownload(format, location, accessionDetailsMap, protocol, asperaConnectLocation, emailId);

    }

}
