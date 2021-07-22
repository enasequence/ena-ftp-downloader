package uk.ac.ebi.ena.cv19fd.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by raheela on 20/04/2021.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum DownloadFormatEnum {
    EMBL(1, "EMBL flatfile", "dat"),
    FASTA(2, "FASTA", "fasta"),
    XML(1, "XML metadata", "xml"),
    FASTQ(2, "FASTQ", null),
    SUBMITTED(3, "SUBMITTED", null);


    private int value;
    private String message;
    private String extension;

    public static DownloadFormatEnum getFormat(List<DownloadFormatEnum> formats, int input) {
        return formats.stream().filter(f -> f.getValue() == input).findFirst().get();
    }
}
