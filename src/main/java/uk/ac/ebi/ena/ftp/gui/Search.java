package uk.ac.ebi.ena.ftp.gui;

import uk.ac.ebi.ena.ftp.model.RemoteFile;

import java.util.List;
import java.util.Map;

/**
 * Created by suranj on 07/06/2017.
 */
public class Search {

    private String accession;

    private Map<String, List<RemoteFile>> reportFileMap;

    public Search(String acc) {
        this.accession = acc;
    }

    public Search(Map<String, List<RemoteFile>> reportFileMap) {
        this.reportFileMap = reportFileMap;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public Map<String, List<RemoteFile>> getReportFileMap() {
        return reportFileMap;
    }

    public void setReportFileMap(Map<String, List<RemoteFile>> reportFileMap) {
        this.reportFileMap = reportFileMap;
    }

    @Override
    public String toString() {
        return "Search{" +
                "accession='" + accession + '\'' +
                ", reportFileMap=" + reportFileMap +
                '}';
    }
}
