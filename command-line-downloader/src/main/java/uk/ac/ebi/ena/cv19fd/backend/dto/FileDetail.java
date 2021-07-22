package uk.ac.ebi.ena.cv19fd.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDetail {
    private String experimentId;
    private String runId;
    private String ftpUrl;
    private Long bytes;
    private String md5;
}
