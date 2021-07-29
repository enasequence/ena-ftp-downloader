package uk.ac.ebi.ena.backend.service;

import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;

import java.util.List;
import java.util.Map;

/**
 * Class that will expose methods to download accessions based on the inputs provided by the user
 * This will be used by the command line tool to start the download
 */
public interface BackendService {

    /**
     * @param format                The format provided by the user
     * @param location              The download location
     * @param accessionDetailsMap   The accessionDetails map
     * @param emailId               The recipient email Id
     * @param protocol              The protocol for download provided by the user
     * @param asperaConnectLocation The location of aspera connect folder if {@link ProtocolEnum} is ASPERA
     * @param emailId               The emailId at which mail will be sent once downloads are completed
     */

    void startDownload(DownloadFormatEnum format, String location, Map<String, List<String>> accessionDetailsMap,
                       ProtocolEnum protocol, String asperaConnectLocation, String emailId);
}
