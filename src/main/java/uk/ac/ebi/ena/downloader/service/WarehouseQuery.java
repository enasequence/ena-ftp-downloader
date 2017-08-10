package uk.ac.ebi.ena.downloader.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.RemoteFile;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by suranj on 27/05/2016.
 */
public class WarehouseQuery {
    public static final String ERA_ANALYSIS_ID_PATTERN = "[ESDR]RZ[0-9]+";
    private final static Logger log = LoggerFactory.getLogger(WarehouseQuery.class);

    final ExecutorService pool = Executors.newFixedThreadPool(1);


    public Future<Map<String, List<RemoteFile>>> doWarehouseSearch(String acc, DownloadSettings.Method method) {
        return this.pool.submit(() -> {
            WarehouseQuery warehouseQuery = new WarehouseQuery();
            Map<String, List<RemoteFile>> map = warehouseQuery.query(acc, method);
            return map;
        });
    }

    public Future<Map<String, List<RemoteFile>>> doPortalSearch(String result, String query, DownloadSettings.Method method) {
        return this.pool.submit(() -> {
            WarehouseQuery warehouseQuery = new WarehouseQuery();
            Map<String, List<RemoteFile>> map = warehouseQuery.portalQuery(result, query, method);
            return map;
        });
    }

    public Map<String, List<RemoteFile>> query(String accession, DownloadSettings.Method method) {
        // URL stump for programmatic query of files
        String resultDomain = getResultDomain(accession);
        String[] types = {};
        if (resultDomain.equals("analysis")) {
            types = new String[]{"submitted"};
        } else {
            types = new String[]{"fastq", "submitted", "sra"};
        }
        String fields = "";
        for (int t = 0; t < types.length; t++) {
            String type = types[t];
            fields += type + "_" + method.name().toLowerCase() + "," + type + "_bytes," + type + "_md5";
            if (t < types.length - 1) {
                fields += ",";
            }
        }
        String url = "http://www.ebi.ac.uk/ena/data/warehouse/filereport?accession=" + accession + "&result=" + resultDomain + "&fields=" + fields;
        try {
            // Build URL, Connect and get results reader
            List<String> fileStrings = null;
            URL enaQuery = new URL(url);
            URLConnection yc = enaQuery.openConnection();
            fileStrings = IOUtils.readLines(yc.getInputStream());
            yc.getInputStream().close();
            if (fileStrings.size() > 1) {
                return parseFileReport(fileStrings, types, 0);
            }
            return new HashMap<>();

        } catch (Exception e) {
            log.warn("Error with warehouse query", e);
        }
        return new HashMap<>();
    }

    public Map<String, List<RemoteFile>> portalQuery(String resultDomain, String query, DownloadSettings.Method method) {
        // URL stump for programmatic query of files
        String[] types = {};
        if (resultDomain.equals("analysis")) {
            types = new String[]{"submitted"};
        } else {
            types = new String[]{"fastq", "submitted", "sra"};
        }
        String fields = "";
        for (int t = 0; t < types.length; t++) {
            String type = types[t];
            fields += type + "_" + method.name().toLowerCase() + "," + type + "_bytes," + type + "_md5";
            if (t < types.length - 1) {
                fields += ",";
            }
        }
        String url = "https://www.ebi.ac.uk/ena/portal/api/search?query=" + query + "&result=" + resultDomain + "&fields=" + fields + "&limit=0&download=true";
        System.out.println(url);
        try {
            // Build URL, Connect and get results reader
            List<String> fileStrings = null;
            URL enaQuery = new URL(url);
            URLConnection yc = enaQuery.openConnection();
            fileStrings = IOUtils.readLines(new BufferedInputStream(yc.getInputStream()));
            System.out.println(fileStrings.size());
            yc.getInputStream().close();
            if (fileStrings.size() > 1) {
                return parseFileReport(fileStrings, types, 1);
            }
            return new HashMap<>();

        } catch (Exception e) {
            log.warn("Error with warehouse query", e);
        }
        return new HashMap<>();
    }

    private String getResultDomain(String accession) {
        if (accession.matches(ERA_ANALYSIS_ID_PATTERN)) {
            return "analysis";
        }
        return "read_run";
    }

    private Map<String, List<RemoteFile>> parseFileReport(List<String> fileStrings, String[] types, int skipFields) {
        Map<String, List<RemoteFile>> map = new HashMap<>();
        try {

            for (String type : types) {
                map.put(type, new ArrayList<>());
            }
            for (int f = 1; f < fileStrings.size(); f++) {// skip header line
                if (StringUtils.isNotBlank(StringUtils.trim(fileStrings.get(f)))) {
                    String[] parts = fileStrings.get(f).split("\\t", -1);// get all elements including trailing empty
                    int typeIndex = skipFields;
                    for (String type : types) {
                        if (parts.length > typeIndex) {
                            List<RemoteFile> files = map.get(type);
                            if (StringUtils.isBlank(parts[0 + typeIndex])) {
                                continue;
                            }
                            if (StringUtils.contains(parts[0 + typeIndex], ";")) {
                                String[] fileParts = parts[0 + typeIndex].split(";");
                                String[] sizeParts = parts[1 + typeIndex].split(";");
                                String[] md5Parts = parts[2 + typeIndex].split(";");
                                for (int p = 0; p < fileParts.length; p++) {
                                    RemoteFile file = new RemoteFile(StringUtils.substringAfterLast(fileParts[p], "/"), Long.parseLong(sizeParts[p]), fileParts[p], md5Parts[p]);
                                    files.add(file);
                                }
                            } else {
                                RemoteFile file = new RemoteFile(StringUtils.substringAfterLast(parts[0 + typeIndex], "/"), Long.parseLong(parts[1 + typeIndex]), parts[0 + typeIndex], parts[2 + typeIndex]);
                                files.add(file);
                            }
                        }
                        typeIndex += 3;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing report", e);
        }
        return map;
    }

}
