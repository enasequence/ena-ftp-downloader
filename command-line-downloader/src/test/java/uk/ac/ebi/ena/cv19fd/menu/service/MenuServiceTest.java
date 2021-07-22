package uk.ac.ebi.ena.cv19fd.menu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.*;
import uk.ac.ebi.ena.cv19fd.app.menu.services.MenuService;
import uk.ac.ebi.ena.cv19fd.app.utils.ScannerUtils;
import uk.ac.ebi.ena.cv19fd.backend.service.BackendService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class MenuServiceTest {

    @Mock
    BackendService backendService;

    @Spy
    @InjectMocks
    private MenuService menuService = new MenuService(new ScannerUtils(), backendService);

    private String path = System.getProperty("user.home");
    private String emailId = "datasubs@ebi.ac.uk";
    private String accessionList = "MN908947,LR991698,MW185475,BS000688";
    private String invalidAccessionList = "MN908947,LR991698,MW185475,21000688";

    @Test
    public void test_buildDomainMenu_when_input_value_is_viral_sequence() {
        String inputData = DomainEnum.VIRAL_SEQUENCES.getValue() + "\n" + DataTypeEnum.SEQUENCES.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.FASTA.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData.getBytes()));
        menuService.aBuildDomainMenu();
    }

    @Test
    public void test_buildDomainMenu_when_input_value_is_host_sequences() {
        String inputData1 = DomainEnum.HOST_SEQUENCES.getValue() + "\n" + DataTypeEnum.HUMAN_READS.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.XML.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_AND_DOWNLOAD.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.aBuildDomainMenu();

    }


    @Test
    public void test_buildDomainMenu_when_input_value_is_help() {
        String inputData1 = DomainEnum.HELP.getValue() + "\n" + DomainEnum.HOST_SEQUENCES.getValue() + "\n" + DataTypeEnum.HUMAN_READS.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.XML.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.aBuildDomainMenu();
    }

    @Test
    public void test_buildDomainMenu_when_input_value_is_privacy_notice() {
        String inputData1 = DomainEnum.PRIVACY.getValue() + "\n" + DomainEnum.HOST_SEQUENCES.getValue() + "\n" + DataTypeEnum.HUMAN_READS.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.XML.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.aBuildDomainMenu();
    }

    @Test
    public void test_buildMenu_when_input_value_is_host_sequences_with_xml_download() {
        String inputData1 = DataTypeEnum.HUMAN_READS.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.XML.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_AND_DOWNLOAD.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.bBuildDataTypeMenu(DomainEnum.HOST_SEQUENCES);

    }

    @Test
    public void test_buildMenu_when_input_value_is_host_sequences_with_FASTQ_download() throws Exception {
        String inputData1 = DataTypeEnum.HUMAN_READS.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.FASTQ.getValue() + "\n" + path + "\n"
                + ProtocolEnum.FTP.getValue() + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_AND_DOWNLOAD.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        Mockito.when(backendService.isDownloadLocationValid(path)).thenReturn(Boolean.TRUE);
        menuService.bBuildDataTypeMenu(DomainEnum.HOST_SEQUENCES);
        verify(backendService, times(1)).isDownloadLocationValid(path);

    }

    @Test
    public void test_buildMenu_when_input_value_sequences_with_list_of_accessions() throws Exception {
        String inputData1 = DataTypeEnum.SEQUENCES.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_LIST.getValue() + "\n" + AccessionsEntryMethodEnum.DOWNLOAD_FROM_LIST.getValue() + "\n" + accessionList + "\n" + DownloadFormatEnum.FASTQ.getValue()
                + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_AND_DOWNLOAD.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        Mockito.when(backendService.isDownloadLocationValid(path)).thenReturn(Boolean.TRUE);
        menuService.bBuildDataTypeMenu(DomainEnum.VIRAL_SEQUENCES);
        verify(backendService, times(1)).isDownloadLocationValid(path);

    }

    @Test
    public void test_buildMenu_when_input_value_is_viral_sequences_with_FASTA_download() throws Exception {
        String inputData1 = DataTypeEnum.SEQUENCES.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.FASTA.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.bBuildDataTypeMenu(DomainEnum.VIRAL_SEQUENCES);
    }

    @Test
    public void test_buildMenu_when_input_value_is_viral_sequences_with_EMBL_download() {
        String inputData1 = DataTypeEnum.SEQUENCED_SAMPLES.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.EMBL.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.bBuildDataTypeMenu(DomainEnum.VIRAL_SEQUENCES);
    }

    @Test
    public void test_buildMenu_when_input_value_is_viral_sequences_with_goback() {
        String inputData1 = "b\n" + DomainEnum.VIRAL_SEQUENCES.getValue() + "\n" +
                DataTypeEnum.SEQUENCED_SAMPLES.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.XML.getValue() + "\n" + path + "\n"
                + emailId + "\n"
                + ActionEnum.CREATE_SCRIPT.getValue() + "\n" + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.bBuildDataTypeMenu(DomainEnum.VIRAL_SEQUENCES);
    }

    @Test
    public void test_buildMenu_with_goback_on_DataFormat_screen() {
        String inputData1 =
                DataTypeEnum.SEQUENCED_SAMPLES.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.XML.getValue() + "\nb\n" +
                        +DownloadFormatEnum.XML.getValue() + "\n" + path + "\n"
                        + emailId + "\n"
                        + ActionEnum.CREATE_SCRIPT.getValue() + "\n" + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.bBuildDataTypeMenu(DomainEnum.VIRAL_SEQUENCES);
    }

    @Test
    public void test_buildMenu_with_invalid_location() {
        String inputData1 =
                DataTypeEnum.SEQUENCED_SAMPLES.getValue() + "\n" + DownloadOptionMenuEnum.DOWNLOAD_ALL.getValue() + "\n" + DownloadFormatEnum.XML.getValue() + "\nb\n" + DownloadFormatEnum.XML.getValue() + "\nlocation\n"
                        + path + "\n" + emailId +
                        ActionEnum.CREATE_SCRIPT.getValue() + "\n" + ActionEnum.CREATE_SCRIPT.getValue() + "\n";
        System.setIn(new java.io.ByteArrayInputStream(inputData1.getBytes()));
        menuService.bBuildDataTypeMenu(DomainEnum.VIRAL_SEQUENCES);
    }

}
