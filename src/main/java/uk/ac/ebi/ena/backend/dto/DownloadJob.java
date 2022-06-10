package uk.ac.ebi.ena.backend.dto;

import lombok.Data;
import uk.ac.ebi.ena.app.menu.enums.AccessionsEntryMethodEnum;

import java.util.ArrayList;
import java.util.List;

@Data
public class DownloadJob {
    private String accessionField = null;
    private List<String> accessionList = new ArrayList<>();
    private AccessionsEntryMethodEnum accessionEntryMethod;

}
