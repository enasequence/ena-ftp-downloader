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

    public List<RemoteFile> queryFastq(String accession) {
        // URL stump for programmatic query of files
        String url = "http://www.ebi.ac.uk/ena/data/warehouse/filereport?accession=" + accession + "&result=read_run&fields=fastq_ftp,fastq_bytes";
        try {
            // Build URL, Connect and get results reader
            List<String> fileStrings = null;
            URL enaQuery = new URL(url);
            URLConnection yc = enaQuery.openConnection();
            fileStrings = IOUtils.readLines(yc.getInputStream());
            yc.getInputStream().close();

            return parseFileReport(fileStrings);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<RemoteFile> parseFileReport(List<String> fileStrings) {
        List<RemoteFile> files = new ArrayList<>();
        for (int f = 1; f < fileStrings.size(); f++) {// skip header line
            String[] parts = fileStrings.get(f).split("\\s");
            RemoteFile file = new RemoteFile(StringUtils.substringAfterLast(parts[0], "/"), Long.parseLong(parts[1]), parts[0]);
            files.add(file);
        }
        return files;
    }
}
