package uk.ac.ebi.ena.ftp.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suranj on 27/05/2016.
 */
public class WarehouseQuery {
    public static final String ERA_ANALYSIS_ID_PATTERN = "[ESDR]RZ[0-9]+";
    private final static Logger log = LoggerFactory.getLogger(WarehouseQuery.class);

    public List<RemoteFile> query(String accession, String type) {
        // URL stump for programmatic query of files
        String resultDomain = getResultDomain(accession);
        if (resultDomain.equals("analysis") && type.equals("fastq")) {
            return new ArrayList<>();
        }
        String url = "http://www.ebi.ac.uk/ena/data/warehouse/filereport?accession=" + accession + "&result=" + resultDomain + "&fields=" + type + "_ftp," + type + "_bytes," + type + "_md5";
        try {
            // Build URL, Connect and get results reader
            List<String> fileStrings = null;
            URL enaQuery = new URL(url);
            URLConnection yc = enaQuery.openConnection();
            fileStrings = IOUtils.readLines(yc.getInputStream());
            yc.getInputStream().close();
            if (fileStrings.size() > 1) {
                return parseFileReport(fileStrings);
            }
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error with warehouse query", e);
        }
        return new ArrayList<>();
    }

    private String getResultDomain(String accession) {
        if (accession.matches(ERA_ANALYSIS_ID_PATTERN)) {
            return "analysis";
        }
        return "read_run";
    }

    private List<RemoteFile> parseFileReport(List<String> fileStrings) {
        List<RemoteFile> files = new ArrayList<>();
        for (int f = 1; f < fileStrings.size(); f++) {// skip header line
            if (StringUtils.isNotBlank(fileStrings.get(f))) {
                String[] parts = fileStrings.get(f).split("\\s");
                if (StringUtils.contains(parts[0], ";")) {
                    String[] fileParts = parts[0].split(";");
                    String[] sizeParts = parts[1].split(";");
                    String[] md5Parts = parts[2].split(";");
                    for (int p = 0; p < fileParts.length; p++) {
                        RemoteFile file = new RemoteFile(StringUtils.substringAfterLast(fileParts[p], "/"), Long.parseLong(sizeParts[p]), fileParts[p], md5Parts[p]);
                        files.add(file);

                    }

                } else {
                    RemoteFile file = new RemoteFile(StringUtils.substringAfterLast(parts[0], "/"), Long.parseLong(parts[1]), parts[0], parts[2]);
                    files.add(file);
                }
            }
        }
        return files;
    }
}
