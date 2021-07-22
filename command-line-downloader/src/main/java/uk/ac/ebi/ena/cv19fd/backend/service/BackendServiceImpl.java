package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;

import java.util.List;

@Component
@Slf4j
public class BackendServiceImpl implements BackendService {

    private final AccessionDetailsService accessionDetailsService;
    private final DownloadLocationValidatorService downloadLocationValidatorService;
    private final FileDownloaderService fileDownloaderService;

    public BackendServiceImpl(AccessionDetailsService accessionDetailsService,
                              DownloadLocationValidatorService downloadLocationValidatorService,
                              FileDownloaderService fileDownloaderService) {
        this.accessionDetailsService = accessionDetailsService;
        this.downloadLocationValidatorService = downloadLocationValidatorService;
        this.fileDownloaderService = fileDownloaderService;
    }

    @Override
    public boolean isDownloadLocationValid(String downloadLocation) {
        return downloadLocationValidatorService.validateDownloadLocation(downloadLocation);
    }

    @Override
    public void startDownload(String downloadLoc, DomainEnum domain, DataTypeEnum dataType, DownloadFormatEnum format
            , String emailId, List<String> accessionList, ProtocolEnum protocol, String asperaLoc) throws Exception {
        log.info("Starting download for domain:{}, dataType:{}, format:{} at download location:{}. Email ID given:{}" +
                ",protocol:{}, asperaLoc:{}", domain, dataType, format, downloadLoc, emailId, protocol, asperaLoc);
        switch (format) {
            case EMBL:
            case XML:
            case FASTA:
                fileDownloaderService.startDownload(downloadLoc, domain, dataType, format, emailId, accessionList);
                break;
            case FASTQ:
            case SUBMITTED:
                accessionDetailsService.fetchAccessionAndDownload(domain, dataType, format, downloadLoc, emailId,
                        accessionList, protocol, asperaLoc);
        }
    }


}
