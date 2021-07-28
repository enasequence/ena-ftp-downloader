package uk.ac.ebi.ena.cv19fd.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnaPortalResponse {

    @JsonProperty("run_accession")
    private String runId;
    @JsonProperty("experiment_accession")
    @JsonAlias({"sample_accession", "study_accession", "analysis_accession", "run_accession"})
    private String parentId;
    @JsonProperty("fastq_bytes")
    @JsonAlias({"submitted_bytes", "generated_bytes"})
    private String bytes;
    @JsonProperty("fastq_ftp")
    @JsonAlias({"submitted_ftp", "fastq_aspera", "submitted_aspera", "generated_ftp", "generated_aspera"})
    private String url;
    @JsonProperty("fastq_md5")
    @JsonAlias({"submitted_md5", "generated_md5"})
    private String md5;
}
