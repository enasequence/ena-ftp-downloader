package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.ac.ebi.ena.app.constants.Constants;

import java.util.Arrays;
import java.util.List;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum DownloadFormatEnum {
    READS_FASTQ(1, "Read files Fastq", Arrays.asList(Constants.RUN,Constants.PROJECT,Constants.SAMPLE,Constants.EXPERIMENT)),
    READS_SUBMITTED(2, "Read files Submitted", Arrays.asList(Constants.RUN,Constants.PROJECT,Constants.SAMPLE,Constants.EXPERIMENT)),
    ANALYSIS_SUBMITTED(3, "Analysis files Submitted",Arrays.asList(Constants.PROJECT,Constants.ANALYSIS)),
    ANALYSIS_GENERATED(4, "Analysis files Generated",Arrays.asList(Constants.PROJECT,Constants.ANALYSIS));

    private int value;
    private String message;
    private List<String> accessionTypes;

    public static DownloadFormatEnum getFormat(List<DownloadFormatEnum> formats, int input) {
        return formats.stream().filter(f -> f.getValue() == input).findFirst().get();
    }
}
