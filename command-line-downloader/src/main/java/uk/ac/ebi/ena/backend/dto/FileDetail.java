package uk.ac.ebi.ena.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDetail {
    private String parentId;
    private String runId;
    private String ftpUrl;
    private Long bytes;
    private String md5;
}
