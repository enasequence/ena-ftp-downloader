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

package uk.ac.ebi.ena.app;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.menu.services.MenuService;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.app.utils.MenuUtils;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.service.BackendService;
import uk.ac.ebi.ena.backend.service.EnaPortalService;

import javax.security.auth.message.AuthException;
import java.io.File;

@Profile("!test")
@Component
@Slf4j
@NoArgsConstructor
public class MainRunner implements CommandLineRunner {

    final Logger console = LoggerFactory.getLogger("console");

    public static final String ASPERA_PATH_MSG = "Please enter the path to your local Aspera Connect/CLI installation" +
            ". This tool will look for 'bin' and 'etc' folders in the provided folder.";
    @Value("${accessions:#{null}}")
    public String accessions;

    @Value("${format:#{null}}")
    public String formatStr;

    @Value("${location:#{null}}")
    public String downloadLocation;

    @Value("${protocol:#{null}}")
    public String protocolStr;

    @Value("${asperaLocation:#{null}}")
    public String asperaLocation;

    @Value("${email:#{null}}")
    public String emailId;

    @Value("${dataHubUsername:#{null}}")
    public String userName;

    @Value("${dataHubPassword:#{null}}")
    public String password;

    MenuService menuBuilder;
    private BackendService backendService;

    private EnaPortalService enaPortalService;

    @Autowired
    MainRunner(MenuService menuBuilder, BackendService backendService, EnaPortalService enaPortalService) {
        this.menuBuilder = menuBuilder;
        this.backendService = backendService;
        this.enaPortalService = enaPortalService;
    }

    public static void exit() {
        System.out.println("Thanks for using the ena-file-downloader! Goodbye!!");
        System.exit(0);
    }

    @Override
    public void run(String... args) {
        try {
            System.out.println(MenuUtils.welcomeMessage);
            CommonUtils.printSeparatorLine();
            if (args.length > 0) {
                System.out.println("Provided parameters:\n" + StringUtils.join(args, "\n"));
            }

            if (args.length >= 5) {
                try {
                    trimInputs(downloadLocation, asperaLocation, accessions, userName);
                    formatStr = StringUtils.trim(formatStr);
                    DownloadFormatEnum format = DownloadFormatEnum.valueOf(formatStr);
                    protocolStr = StringUtils.trim(protocolStr);
                    ProtocolEnum protocol = ProtocolEnum.valueOf(protocolStr.toUpperCase());
                    File dLoc = new File(downloadLocation);
                    AuthenticationDetail authenticationDetail = null;
                    if (!dLoc.exists() || !dLoc.canWrite()) {
                        System.out.println(dLoc + " does not exists or is read only.");
                    } else {
                        if (accessions != null && new File(downloadLocation).exists() && new File(downloadLocation).canWrite()) {
                            if (protocol == ProtocolEnum.ASPERA) {
                                Assert.notNull(asperaLocation, ASPERA_PATH_MSG);
                            }
                            //Download the data from dataHub
                            if (StringUtils.isNotBlank(userName)) {
                                authenticationDetail = new AuthenticationDetail();
                                if (!StringUtils.startsWith(userName, "dcc_")) {
                                    System.out.println("Please provide a valid data hub name (dcc_username)");
                                    log.error("Invalid data hub name {} (dcc user) provided. ", userName);
                                    throw new IllegalArgumentException("Invalid data hub name");
                                } else if (!"FTP".equals(protocol.name())) {
                                    System.out.println("Only FTP protocol is supported to download the files from a data hub");
                                    log.error("Only FTP protocol is supported to download the files from a data hub. Provided protocol is {}", protocol);
                                    throw new IllegalArgumentException("Only FTP protocol is supported to download the files from a data hub");
                                }
                                authenticationDetail.setUserName(userName);
                                password = StringUtils.trim(password);
                                authenticationDetail.setPassword(password);
                                //Validate username and password
                                if (!enaPortalService.authenticateUser(authenticationDetail)) {
                                    log.error("Data hub username and/or password is incorrect.");
                                    throw new AuthException("Data hub authentication failed");
                                }

                            }
                            emailId = StringUtils.trim(emailId);
                            backendService.startDownload(format, downloadLocation,
                                    MenuUtils.parseAccessions(accessions), protocol, asperaLocation,
                                    emailId, authenticationDetail);
                        }
                        console.info("Downloads Completed");
                    }
                } catch (IllegalArgumentException iae) {
                    System.out.println("Invalid/insufficient parameters provided. Please select your options.");
                    log.error("Invalid/insufficient parameters provided.", iae);
                    menuBuilder.showTypeOfDataMenu();
                } catch (Exception e) {
                    log.error("Exception Occurred while downloading", e);
                }
            } else {
                if (args.length > 0) {
                    log.error("Not enough parameters provided. Starting interactive flow..");
                    System.out.println("Not enough parameters provided. Starting interactive flow..");
                }
                menuBuilder.showTypeOfDataMenu();

            }
        } catch (Exception e) {
            log.error("Exception Occurred ", e);
            System.out.print("Something went wrong! Please report to the EMBL-EBI ENA helpdesk at " +
                    "https://www.ebi.ac.uk/ena/browser/support . Please provide the contents of the app.log file.");
        }
    }

    private void trimInputs(String downloadLocation, String asperaLocation, String accessions, String userName) {
        this.downloadLocation = StringUtils.trim(downloadLocation);
        this.asperaLocation = StringUtils.trim(asperaLocation);
        this.accessions = StringUtils.trim(accessions);
        this.userName = StringUtils.trim(userName);
    }


}
