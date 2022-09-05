package uk.ac.ebi.ena.app.menu.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.app.menu.enums.AccessionsEntryMethodEnum;
import uk.ac.ebi.ena.app.menu.enums.ActionEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.ScannerUtils;
import uk.ac.ebi.ena.backend.service.BackendService;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class MenuServiceTest {

    @Mock
    BackendService backendService;

    @Spy
    @InjectMocks
    private MenuService menuService = new MenuService(new ScannerUtils(), backendService, null);

    @Test
    public void testBuildAccessionEntryMenu_WhenFtpProtocol() {
        String accessionList = "SRX6415697,SRX2000905,SRX6415695";
        String path = System.getProperty("user.home");
        String emailId = "datasubs@ebi.ac.uk";
        //ARRANGE
        String inputData = AccessionsEntryMethodEnum.DOWNLOAD_FROM_LIST.getValue() + "\n" + accessionList + "\n" + DownloadFormatEnum.READS_FASTQ.getValue() + "\n" + path + "\n" +
                ProtocolEnum.FTP.getValue() + "\n" + emailId + "\n"
                + ActionEnum.CREATE_AND_DOWNLOAD.getValue();
        System.setIn(new java.io.ByteArrayInputStream(inputData.getBytes()));
        //ACT
        menuService.aBuildAccessionEntryMenu(null);
    }

    @Test
    public void testBuildAccessionEntryMenu_WhenValidAccessionFile() {
        String accessionFile = "src/test/resources/accessionFile";

        String path = System.getProperty("user.home");
        String emailId = "datasubs@ebi.ac.uk";
        //ARRANGE
        String inputData = AccessionsEntryMethodEnum.DOWNLOAD_FROM_FILE.getValue() + "\n" + accessionFile + "\n" + DownloadFormatEnum.READS_FASTQ.getValue() + "\n" + path + "\n" +
                ProtocolEnum.FTP.getValue() + "\n" + emailId + "\n"
                + ActionEnum.CREATE_AND_DOWNLOAD.getValue();
        System.setIn(new java.io.ByteArrayInputStream(inputData.getBytes()));
        //ACT
        menuService.aBuildAccessionEntryMenu(null);
    }

    @Test
    public void testBuildAccessionEntryMenu_WhenEmptyAccessionFile() {
        String emptyAccessionFile = "src/test/resources/accFile_empty";
        String accessionFile = "src/test/resources/accessionFile";


        String path = System.getProperty("user.home");
        String emailId = "datasubs@ebi.ac.uk";
        //ARRANGE
        String inputData = AccessionsEntryMethodEnum.DOWNLOAD_FROM_FILE.getValue() + "\n" + emptyAccessionFile + "\n" + accessionFile + "\n" + DownloadFormatEnum.READS_FASTQ.getValue() + "\n" + path + "\n" +
                ProtocolEnum.FTP.getValue() + "\n" + emailId + "\n"
                + ActionEnum.CREATE_AND_DOWNLOAD.getValue();
        System.setIn(new java.io.ByteArrayInputStream(inputData.getBytes()));
        //ACT
        menuService.aBuildAccessionEntryMenu(null);
    }
}
