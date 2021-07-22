package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class DownloadLocationValidatorService {
    /**
     * API will check if the download location provided by the user is valid, has write permissions,
     * has enough space to accomodate all the files
     * For XML/EMBL/FASTA formats, we will check if the user has 20GB space available at the downloadLoc
     * For FASTQ & submitted formats, we will call the portal API and calculate the total space needed.
     *
     * @param downloadLocation the download location provided by the user
     * @return true if download can be started at the provided location
     */
    public boolean validateDownloadLocation(String downloadLocation) {

        return isValidDownloadLocation(downloadLocation) && hasWritePermission(downloadLocation);
    }

    private boolean isValidDownloadLocation(String downloadLocation) {
        return new File(downloadLocation).exists();
    }

    private boolean hasWritePermission(String downloadLocation) {
        return new File(downloadLocation).canWrite();
    }

}
