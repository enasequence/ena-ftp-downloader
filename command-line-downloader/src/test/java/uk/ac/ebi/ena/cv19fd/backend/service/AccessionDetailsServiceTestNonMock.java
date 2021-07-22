package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class AccessionDetailsServiceTestNonMock {

    @Autowired
    private AccessionDetailsService accessionDetailsService;

    /**
     * provide local aspera path and download location
     */
    @Disabled
    @Test
    public void testAspera() {
        try {
            this.accessionDetailsService.fetchAccessionAndDownload(DomainEnum.HOST_SEQUENCES, DataTypeEnum.HUMAN_READS, DownloadFormatEnum.FASTQ,
                    "C:/data-files", null, Arrays.asList("SRX8834136", "SRX8834137", "SRX8834138", "SRX8834139", "ERX4219953",
                            "ERX4219954", "ERX4219955", "ERX4219956", "ERX4219957", "ERX4219958", "ERX4219959", "ERX4219960", "ERX4219961", "ERX4219962"), ProtocolEnum.ASPERA,
                    "C:/devtools/aspera-cli");
        } catch (Exception e) {
            log.error("error:", e);
        }
    }
}
