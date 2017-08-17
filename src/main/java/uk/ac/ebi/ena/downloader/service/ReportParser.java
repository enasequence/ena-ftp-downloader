package uk.ac.ebi.ena.downloader.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.RemoteFile;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportParser {
    private final static Logger log = LoggerFactory.getLogger(ReportParser.class);

    final ExecutorService pool = Executors.newFixedThreadPool(1);

    public Map<String, List<RemoteFile>> parseExternalReportFile(File reportFile, DownloadSettings.Method method) {
        Map<String, List<RemoteFile>> map = new HashMap<>();
        int accIndex = -1;
        int fastqIndex = -1;
        int fastqBytesIndex = -1;
        int fastqMd5Index = -1;

        int submittedIndex = -1;
        int submittedBytesIndex = -1;
        int submittedMd5Index = -1;

        int sraIndex = -1;
        int sraBytesIndex = -1;
        int sraMd5Index = -1;

        try {
            List<String> lines = IOUtils.readLines(new FileReader(reportFile));
            String[] headersSplit = StringUtils.splitPreserveAllTokens(lines.get(0), "\t");
            List<String> headers = Arrays.asList(headersSplit);

            accIndex = headers.indexOf("run_accession");
            if (accIndex < 0) {
                accIndex = headers.indexOf("analysis_accession");
            }

            fastqIndex = headers.indexOf("fastq_" + method.name().toLowerCase());
            if (fastqIndex > -1) {
                map.put("fastq", new ArrayList<RemoteFile>());
                fastqBytesIndex = headers.indexOf("fastq_bytes");
                fastqMd5Index = headers.indexOf("fastq_md5");
            }

            submittedIndex = headers.indexOf("submitted_" + method.name().toLowerCase());
            if (submittedIndex > -1) {
                map.put("submitted", new ArrayList<RemoteFile>());
                submittedBytesIndex = headers.indexOf("submitted_bytes");
                submittedMd5Index = headers.indexOf("submitted_md5");
            }

            sraIndex = headers.indexOf("sra_" + method.name().toLowerCase());
            if (sraIndex > -1) {
                map.put("sra", new ArrayList<RemoteFile>());
                sraBytesIndex = headers.indexOf("sra_bytes");
                sraMd5Index = headers.indexOf("sra_md5");
            }

            if (!map.isEmpty()) {
                for (int r = 1; r < lines.size(); r++) {
                    String[] fields = StringUtils.splitPreserveAllTokens(lines.get(r), "\t");
                    if (fastqIndex > -1) {
                        String fastqFilesStr = fields[fastqIndex];
                        if (StringUtils.isNotBlank(fastqFilesStr)) {
                            String[] files = StringUtils.split(fastqFilesStr, ";");
                            String[] bytes = null;
                            String[] md5s = null;
                            for (int f = 0; f < files.length; f++) {
                                String fastqFile = files[f];
                                if (fastqBytesIndex > -1) {
                                    bytes = StringUtils.split(fields[fastqBytesIndex], ";");
                                }
                                if (fastqMd5Index > -1) {
                                    md5s = StringUtils.split(fields[fastqMd5Index], ";");
                                }
                                RemoteFile remoteFile = new RemoteFile(StringUtils.substringAfterLast(fastqFile, "/"),
                                        fastqBytesIndex > -1 ? Long.parseLong(bytes[f]) : 0, fastqFile,
                                        fastqMd5Index > -1 ? md5s[f] : null, fields[accIndex]);
//                                        log.info(remoteFile.toString());
                                map.get("fastq").add(remoteFile);
                            }
                        }
                    }
                    if (submittedIndex > -1) {
                        String submittedFilesStr = fields[submittedIndex];
                        if (StringUtils.isNotBlank(submittedFilesStr)) {
                            String[] files = StringUtils.split(submittedFilesStr, ";");
                            String[] bytes = null;
                            String[] md5s = null;
                            for (int f = 0; f < files.length; f++) {
                                String submittedFile = files[f];
                                if (submittedBytesIndex > -1) {
                                    bytes = StringUtils.split(fields[submittedBytesIndex], ";");
                                }
                                if (submittedMd5Index > -1) {
                                    md5s = StringUtils.split(fields[submittedMd5Index], ";");
                                }
                                map.get("submitted").add(new RemoteFile(StringUtils.substringAfterLast(submittedFile, "/"),
                                        submittedBytesIndex > -1 ? Long.parseLong(bytes[f]) : 0, submittedFile,
                                        submittedMd5Index > -1 ? md5s[f] : null, fields[accIndex]));
                            }
                        }
                        if (sraIndex > -1) {
                            String sraFilesStr = fields[sraIndex];
                            if (StringUtils.isNotBlank(sraFilesStr)) {
                                String[] files = StringUtils.split(sraFilesStr, ";");
                                String[] bytes = null;
                                String[] md5s = null;
                                for (int f = 0; f < files.length; f++) {
                                    String sraFile = files[f];
                                    if (sraBytesIndex > -1) {
                                        bytes = StringUtils.split(fields[sraBytesIndex], ";");
                                    }
                                    if (sraMd5Index > -1) {
                                        md5s = StringUtils.split(fields[sraMd5Index], ";");
                                    }
                                    map.get("sra").add(new RemoteFile(StringUtils.substringAfterLast(sraFile, "/"),
                                            sraBytesIndex > -1 ? Long.parseLong(bytes[f]) : 0, sraFile,
                                            sraMd5Index > -1 ? md5s[f] : null, fields[accIndex]));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error", e);
        }
        return map;
//        });
    }

}
