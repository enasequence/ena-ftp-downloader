package uk.ac.ebi.ena.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BackendServiceImpl implements BackendService {

    private final AccessionDetailsService accessionDetailsService;

    public BackendServiceImpl(AccessionDetailsService accessionDetailsService) {
        this.accessionDetailsService = accessionDetailsService;
    }

    @Override
    public void startDownload(DownloadFormatEnum format, String location, Map<String, List<String>> accessionDetailsMap,
                              ProtocolEnum protocol, String asperaConnectLocation, String emailId) {

        log.info("Starting download for format:{} at download location:{},protocol:{}, asperaLoc:{}, emailId:{}",
                format, location, protocol, asperaConnectLocation, emailId);
        accessionDetailsService.fetchAccessionAndDownload(format, location, accessionDetailsMap, protocol
                , asperaConnectLocation, emailId);
    }
}
