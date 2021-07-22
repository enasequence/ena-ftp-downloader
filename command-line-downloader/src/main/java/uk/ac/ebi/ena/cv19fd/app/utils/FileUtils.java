package uk.ac.ebi.ena.cv19fd.app.utils;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.util.CollectionUtils;
import uk.ac.ebi.ena.cv19fd.Cv19FileDownloaderApplication;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by raheela on 27/04/2021.
 */
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

    public static String getScriptPath(DomainEnum domain, DataTypeEnum dataType,
                                       DownloadFormatEnum format, List<String> accessionList) {
        String path = getJarDir() + File.separator + "download_" + domain + "-" + dataType + "-" + format;
        if (CollectionUtils.isEmpty(accessionList)) {
            path += "-all";
        } else {
            path += "-list";
        }
        return path += getScriptExtension();
    }

    public static void createDownloadScript(String location, DomainEnum domain, DataTypeEnum dataType,
                                            DownloadFormatEnum format, String emailId, List<String> accessionList
            , ProtocolEnum protocol, String asperaLocation) {
        File file = new File(location);
        if (file.exists()) {
            File file1 = new File(getScriptPath(domain, dataType, format, accessionList));
            try (FileOutputStream fileOut = new FileOutputStream(file1)) {
                if (asperaLocation != null && asperaLocation.contains(" ")) {
                    asperaLocation = addDoubleQuotes(Paths.get(asperaLocation));
                }
                if (accessionList != null && accessionList.size() > 0) {
                    String content =
                            "java -jar " + getJarPath() + " --domain=" + domain + " " +
                                    "--datatype=" + dataType + " " +
                                    "--format=" + format + " --location=" + location + " --email=" + emailId + " --accessions=" + StringUtils.join(accessionList, ',')
                                    + " --protocol=" + protocol + " --asperaLocation=" + asperaLocation;
                    log.info("content:{}", content);
                    fileOut.write(content.getBytes());
                } else {
                    String content =
                            "java -jar " + getJarPath() + " --domain=" + domain + " " +
                                    "--datatype=" + dataType + " " +
                                    "--format=" + format + " --location=" + location + " --email=" + emailId +
                                    " --protocol=" + protocol + " --asperaLocation=" + asperaLocation;
                    ;
                    log.info("content:{}", content);
                    fileOut.write(content.getBytes());
                }

                file1.setExecutable(true);
            } catch (Exception e) {
                log.error("Exception occurred while creating download script", e);
                System.out.println("Exception occurred while creating download script : " + e.getMessage());
            }
        }

    }

    public static String getScriptExtension() {
        return SystemUtils.IS_OS_WINDOWS ? ".bat" : ".sh";
    }

    @SneakyThrows
    private static String getJarDir() {
        ApplicationHome home = new ApplicationHome(Cv19FileDownloaderApplication.class);
        return home.getDir().getAbsolutePath();    // returns the folder where the jar is. This is what I wanted.

    }

    private static String getJarPath() {
        ApplicationHome home = new ApplicationHome(Cv19FileDownloaderApplication.class);
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
}

