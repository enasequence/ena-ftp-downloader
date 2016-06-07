package uk.ac.ebi.ena.ftp.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suranj on 27/05/2016.
 */
public class WarehouseQuery {

    public List<RemoteFile> query(String accession, String type) {
        // URL stump for programmatic query of files
        String url = "http://www.ebi.ac.uk/ena/data/warehouse/filereport?accession=" + accession + "&result=read_run&fields=" + type + "_ftp," + type + "_bytes," + type + "_md5";
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
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<RemoteFile> parseFileReport(List<String> fileStrings) {
        List<RemoteFile> files = new ArrayList<>();
        for (int f = 1; f < fileStrings.size(); f++) {// skip header line
            if (StringUtils.isNotBlank(fileStrings.get(f))) {
                String[] parts = fileStrings.get(f).split("\\s");
                RemoteFile file = new RemoteFile(StringUtils.substringAfterLast(parts[0], "/"), Long.parseLong(parts[1]), parts[0], parts[2]);
                files.add(file);
            }
        }
        return files;
    }
}
