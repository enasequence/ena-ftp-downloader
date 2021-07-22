package uk.ac.ebi.ena.cv19fd.app;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.services.MenuService;
import uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils;
import uk.ac.ebi.ena.cv19fd.app.utils.FileUtils;
import uk.ac.ebi.ena.cv19fd.app.utils.MenuUtils;
import uk.ac.ebi.ena.cv19fd.backend.service.BackendService;

import java.util.Arrays;

import static uk.ac.ebi.ena.cv19fd.app.utils.MenuUtils.parseAccessions;

@Profile("!test")
@Component
@Slf4j
@NoArgsConstructor
public class MainRunner implements CommandLineRunner {

    @Value("${domain:#{null}}")
    public String domainStr;

    @Value("${datatype:#{null}}")
    public String datatypeStr;

    @Value("${format:#{null}}")
    public String formatStr;

    @Value("${location:#{null}}")
    public String downloadLocation;

    @Value("${email:#{null}}")
    public String emailId;

    @Value("${accessions:#{null}}")
    public String accessions;

    @Value("${protocol:#{null}}")
    public String protocolStr;

    @Value("${asperaLocation:#{null}}")
    public String asperaLocation;


    MenuService menuBuilder;
    private BackendService backendService;

    @Autowired
    MainRunner(MenuService menuBuilder, BackendService backendService) {
        this.menuBuilder = menuBuilder;
        this.backendService = backendService;
    }

    public static void exit() {
        System.out.println("Thanks for using the cdp-file-downloader! Goodbye!!");
        System.exit(0);
    }

    @Override
    public void run(String... args) {
        try {
            System.out.println(MenuUtils.welcomeMessage);
            CommonUtils.printSeparatorLine();

            if (args.length >= 5) {
                try {
                    DomainEnum domain = DomainEnum.valueOf(domainStr);
                    DataTypeEnum dataTypeEnum = DataTypeEnum.valueOf(datatypeStr);
                    DownloadFormatEnum format = DownloadFormatEnum.valueOf(formatStr);
                    ProtocolEnum protocol = ProtocolEnum.valueOf(protocolStr);

                    if (domain != null && dataTypeEnum != null && format != null) {
                        backendService.startDownload(downloadLocation, domain, dataTypeEnum, format, emailId,
                                parseAccessions(accessions), protocol, asperaLocation);
                    }
                    log.info("Downloads Completed");
                } catch (IllegalArgumentException iae) {
                    System.out.println("Invalid/insufficient parameters provided. Please select your options.");
                    log.error("Invalid/insufficient parameters provided.", iae);
                    menuBuilder.aBuildDomainMenu();
                } catch (Exception e) {
                    log.error("Exception Occurred while downloading", e);
                }
            } else {
                menuBuilder.aBuildDomainMenu();

            }
        } catch (Exception e) {
            FileUtils.writeExceptionToFile("Exception Occurred " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            log.error("Exception Occurred ", e);
            System.out.print("Something went wrong! Please report to the EMBL-EBI ENA helpdesk at " +
                    "https://www.ebi.ac.uk/ena/browser/support . Please provide the contents of the app.log file.");
        }
    }


}
