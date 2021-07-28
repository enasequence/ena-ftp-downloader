package uk.ac.ebi.ena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.ebi.ena.app.exceptions.GlobalExceptionHandler;

@SpringBootApplication
public class EnaFileDownloaderApplication {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        SpringApplication.run(EnaFileDownloaderApplication.class, args);
    }

}
