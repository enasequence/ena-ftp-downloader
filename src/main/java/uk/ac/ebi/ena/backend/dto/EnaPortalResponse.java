/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnaPortalResponse {

    @JsonProperty("run_accession")
    @JsonAlias("analysis_accession")
    private String recordId;
    @JsonProperty("experiment_accession")
    @JsonAlias({"sample_accession", "study_accession", "analysis_accession", "run_accession"})
    private String parentId;
    @JsonProperty("fastq_bytes")
    @JsonAlias({"submitted_bytes", "generated_bytes", "bam_bytes"})
    private String bytes;
    @JsonProperty("fastq_ftp")
    @JsonAlias({"submitted_ftp", "fastq_aspera", "submitted_aspera", "generated_ftp", "generated_aspera", "bam_ftp"})
    private String url;
    @JsonProperty("fastq_md5")
    @JsonAlias({"submitted_md5", "generated_md5", "bam_md5"})
    private String md5;
}
