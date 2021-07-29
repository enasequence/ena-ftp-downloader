package uk.ac.ebi.ena.app.utils;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.system.ApplicationHome;
import uk.ac.ebi.ena.EnaFileDownloaderApplication;
import uk.ac.ebi.ena.app.menu.enums.AccessionsEntryMethodEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static uk.ac.ebi.ena.app.constants.Constants.ACCESSION_LIST;
import static uk.ac.ebi.ena.app.constants.Constants.ACCESSION_TYPE;

@Slf4j
public class FileUtils {

    private static final String fileName = "error_report.txt";
    private static final String filePath = System.getProperty("user.home");

    private static String absolutePath(String basePath, String pathToFile) {
        return Paths.get(basePath, pathToFile).toAbsolutePath().toString();
    }

    public static void writeExceptionToFile(Object content) {
        writeObjectTOFile(content, fileName);
    }

    private static void writeObjectTOFile(Object content, String fileName) {
        try {
            FileOutputStream fileOut = new FileOutputStream(absolutePath(filePath, fileName));
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(content);
        } catch (Exception e) {
            log.error("Exception occurred while writing to file", e);
            System.out.println("Exception while writing file : " + e.getMessage());
        }
    }


    public static String getScriptPath(Map<String, List<String>> accessionDetailsMap, DownloadFormatEnum format) {
        String accessionType = accessionDetailsMap.get(ACCESSION_TYPE).get(0);
        return getJarDir() + File.separator + "download_" + accessionType + "-" + format + getScriptExtension();
    }

    public static String getScriptExtension() {
        return SystemUtils.IS_OS_WINDOWS ? ".bat" : ".sh";
    }

    @SneakyThrows
    private static String getJarDir() {
        ApplicationHome home = new ApplicationHome(EnaFileDownloaderApplication.class);
        return home.getDir().getAbsolutePath();    // returns the folder where the jar is. This is what I wanted.

    }

    private static String getJarPath() {
        ApplicationHome home = new ApplicationHome(EnaFileDownloaderApplication.class);
        return home.getSource().getAbsolutePath();    // returns the folder where the jar is. This is what I wanted.
    }

    public static boolean isDirectoryExists(String directoryPath) {
        if (Paths.get(directoryPath).toFile().isDirectory()) {
            return true;
        } else {
            System.out.println("Your provided location is not valid !!");
            return false;
        }
    }

    public static String addDoubleQuotes(Path str) {
        return "\"" + str + "\"";
    }

    public static void createDownloadScript(Map<String, List<String>> accessionDetailsMap, DownloadFormatEnum format,
                                            String location, ProtocolEnum protocol, String asperaLocation,
                                            String emailId) {
        File file = new File(location);
        if (file.exists()) {
            File file1 = new File(getScriptPath(accessionDetailsMap, format));
            try (FileOutputStream fileOut = new FileOutputStream(file1)) {
                if (asperaLocation != null && asperaLocation.contains(" ")) {
                    asperaLocation = addDoubleQuotes(Paths.get(asperaLocation));
                }
                List<String> accessionList;
                if (accessionDetailsMap.containsKey(AccessionsEntryMethodEnum.DOWNLOAD_FROM_FILE)) {
                    accessionList = accessionDetailsMap.get(AccessionsEntryMethodEnum.DOWNLOAD_FROM_FILE);
                } else {
                    accessionList = accessionDetailsMap.get(ACCESSION_LIST);
                }
                String content =
                        "java -jar " + getJarPath() + " --accessions=" + StringUtils.join(accessionList, ',') +
                                " --format=" + format + " --location=" + location + " --protocol=" + protocol +
                                " --asperaLocation=" + asperaLocation + " --email=" + emailId;
                log.info("content:{}", content);
                fileOut.write(content.getBytes());

                file1.setExecutable(true);
            } catch (Exception e) {
                log.error("Exception occurred while creating download script", e);
                System.out.println("Exception occurred while creating download script : " + e.getMessage());
            }
        }

    }
}

