package uk.ac.ebi.ena.cv19fd.backend.service;

import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;

import java.util.List;

/**
 * Class that will expose methods to check if download location is valid and download a file based on the inputs provided by the user
 * This will be used by the command line tool to start the download
 */
//cv19-fle-downloader --domain “Viral_SEQUENCES” --dataType “rawReads” --format “FASTQ”   --downloadLoc “C:\\Users\suman\\Downloads”--same as ENUM name
public interface BackendService {

    /**
     * API will check if the download location provided by the user is valid, has write permissions,
     * has enough space to accomodate all the files
     * For XML/EMBL/FASTA formats, we will check if the user has 20GB space available at the downloadLoc
     * For FASTQ & submitted formats, we will call the browser API and calculate the total space needed.
     *
     * @param downloadLocation the download location provided by the user
     * @return true if the download location is valid, the location has write permissions, there is enough space at the location to accomodate all the files
     */
    boolean isDownloadLocationValid(String downloadLocation) throws Exception;


    /**
     * API will start the download for each file. For XML,FASTA and EMBL format, it will invoke the browser API to trigger the download.
     * Copy the full stream. if the stream copy goes okay, we assume that download was successful for XML,FASTA,EMBL formats.
     * For FASTQ,SUBMITTED format, it will call the idlist endpoint to fetch the IDs and then call ebi search endpoint to
     * get the details about each of the accession and start FTP download
     *
     * @param downloadLocation the download location provided by the user
     * @param domain           The domain provided by the user
     * @param dataType         The dataType provided by the user
     * @param format           The format provided by the user
     * @param emailId          The recipient email Id
     * @param accessionList    List of accession ids
     * @param protocol          The protocol provided by the user
     * @param asperaLocation    The location of aspera connect folder if {@link ProtocolEnum} is ASPERA
     */
    void startDownload(String downloadLocation, DomainEnum domain, DataTypeEnum dataType, DownloadFormatEnum format,
                       String emailId, List<String> accessionList, ProtocolEnum protocol, String asperaLocation) throws Exception;


}
