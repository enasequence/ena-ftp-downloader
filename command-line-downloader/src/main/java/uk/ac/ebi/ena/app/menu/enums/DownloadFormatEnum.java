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
    ANALYSIS_SUBMITTED(3, "Analysis files Submitted", Arrays.asList(AccessionTypeEnum.STUDY.getAccessionField(), AccessionTypeEnum.ANALYSIS.getAccessionField())),
    ANALYSIS_GENERATED(4, "Analysis files Generated", Arrays.asList(AccessionTypeEnum.STUDY.getAccessionField(), AccessionTypeEnum.ANALYSIS.getAccessionField()));

    private int value;
    private String message;
    private List<String> accessionTypes;

    public static DownloadFormatEnum getFormat(List<DownloadFormatEnum> formats, int input) {
        return formats.stream().filter(f -> f.getValue() == input).findFirst().get();
    }
}
