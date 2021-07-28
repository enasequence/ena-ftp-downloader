package uk.ac.ebi.ena.app;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.ac.ebi.ena.backend.service.BackendService;

import java.io.File;

@Profile("!test")
@Component
@Slf4j
@NoArgsConstructor
public class MainRunner implements CommandLineRunner {

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

    MenuService menuBuilder;
    private BackendService backendService;

    @Autowired
    MainRunner(MenuService menuBuilder, BackendService backendService) {
        this.menuBuilder = menuBuilder;
        this.backendService = backendService;
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

            if (args.length >= 5) {
                try {
                    DownloadFormatEnum format = DownloadFormatEnum.valueOf(formatStr);
                    ProtocolEnum protocol = ProtocolEnum.valueOf(protocolStr);

                    if (accessions != null && new File(downloadLocation).exists() && new File(downloadLocation).canWrite()) {
                        if (protocol == ProtocolEnum.ASPERA) {
                            Assert.notNull(asperaLocation, "Please enter the path to your local Aspera Connect/CLI installation");
                        }
                        backendService.startDownload(format, downloadLocation, MenuUtils.parseAccessions(accessions), protocol,
                                asperaLocation, emailId);
                    }
                    log.info("Downloads Completed");
                } catch (IllegalArgumentException iae) {
                    System.out.println("Invalid/insufficient parameters provided. Please select your options.");
                    log.error("Invalid/insufficient parameters provided.", iae);
                    menuBuilder.aBuildAccessionEntryMenu();
                } catch (Exception e) {
                    log.error("Exception Occurred while downloading", e);
                }
            } else {
                menuBuilder.aBuildAccessionEntryMenu();

            }
        } catch (Exception e) {
            log.error("Exception Occurred ", e);
            System.out.print("Something went wrong! Please report to the EMBL-EBI ENA helpdesk at " +
                    "https://www.ebi.ac.uk/ena/browser/support . Please provide the contents of the app.log file.");
        }
    }


}
