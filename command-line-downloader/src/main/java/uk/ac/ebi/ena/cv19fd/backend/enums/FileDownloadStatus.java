package uk.ac.ebi.ena.cv19fd.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uk.ac.ebi.ena.cv19fd.backend.dto.FileDetail;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
@Setter
public class FileDownloadStatus {

    private int total;
    private int successsful;

    private List<FileDetail> failedFiles;

}
