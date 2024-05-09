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

package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum DownloadFormatEnum {
    READS_FASTQ(1, "Read files Fastq", Arrays.asList(AccessionTypeEnum.RUN.getAccessionField(), AccessionTypeEnum.STUDY.getAccessionField(), AccessionTypeEnum.SAMPLE.getAccessionField(), AccessionTypeEnum.EXPERIMENT.getAccessionField())),
    READS_SUBMITTED(2, "Read files Submitted", Arrays.asList(AccessionTypeEnum.RUN.getAccessionField(), AccessionTypeEnum.STUDY.getAccessionField(), AccessionTypeEnum.SAMPLE.getAccessionField(), AccessionTypeEnum.EXPERIMENT.getAccessionField())),
    READS_BAM(3, "Read files Bam", Arrays.asList(AccessionTypeEnum.RUN.getAccessionField(), AccessionTypeEnum.STUDY.getAccessionField(), AccessionTypeEnum.SAMPLE.getAccessionField(), AccessionTypeEnum.EXPERIMENT.getAccessionField())),
    ANALYSIS_SUBMITTED(4, "Analysis files Submitted", Arrays.asList(AccessionTypeEnum.STUDY.getAccessionField(), AccessionTypeEnum.ANALYSIS.getAccessionField())),
    ANALYSIS_GENERATED(5, "Analysis files Generated", Arrays.asList(AccessionTypeEnum.STUDY.getAccessionField(), AccessionTypeEnum.ANALYSIS.getAccessionField()));

    private int value;
    private String message;
    private List<String> accessionTypes;

    public static DownloadFormatEnum getFormat(List<DownloadFormatEnum> formats, int input) {
        return formats.stream().filter(f -> f.getValue() == input).findFirst().get();
    }
}
