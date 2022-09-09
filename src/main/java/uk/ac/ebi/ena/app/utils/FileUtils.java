/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.app.utils;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import uk.ac.ebi.ena.EnaFileDownloaderApplication;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.dto.DownloadJob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Slf4j
public class FileUtils {

    static final Logger console = LoggerFactory.getLogger("console");

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


    public static String getScriptPath(DownloadJob downloadJob, DownloadFormatEnum format) {
        String accessionType = downloadJob.getAccessionField();
        return getJarDir() + File.separator + "download_" + StringUtils.substringBefore(accessionType, "_")
                + "-" + format + getScriptExtension();
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

    public static void createDownloadScript(DownloadJob downloadJob, DownloadFormatEnum format,
                                            String location, ProtocolEnum protocol, String asperaLocation,
                                            String emailId, AuthenticationDetail authenticationDetail) {
        File file = new File(location);
        if (file.exists()) {
            File file1 = new File(getScriptPath(downloadJob, format));
            try (FileOutputStream fileOut = new FileOutputStream(file1)) {
                if (asperaLocation != null && asperaLocation.contains(" ")) {
                    asperaLocation = addDoubleQuotes(Paths.get(asperaLocation));
                }
                List<String> accessionList = downloadJob.getAccessionList();

                String content =
                        "java -jar " + getJarPath() + " --accessions=" + StringUtils.join(accessionList, ',') +
                                " --format=" + format + " --location=" + location + " --protocol=" + protocol +
                                " --asperaLocation=" + asperaLocation + " --email=" + emailId +
                                (Objects.nonNull(authenticationDetail) ?
                                        " --dataHubUsername=" + authenticationDetail.getUserName() +
                                                " --dataHubPassword=" + authenticationDetail.getPassword()
                                        : "");
                console.info("script content:{}", content);
                fileOut.write(content.getBytes());

                file1.setExecutable(true);
            } catch (Exception e) {
                log.error("Exception occurred while creating download script", e);
                System.out.println("Exception occurred while creating download script : " + e.getMessage());
            }
        }

    }
}

