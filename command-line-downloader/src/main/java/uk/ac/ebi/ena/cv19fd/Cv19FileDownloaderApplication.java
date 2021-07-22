package uk.ac.ebi.ena.cv19fd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.ebi.ena.cv19fd.app.exceptions.GlobalExceptionHandler;

@SpringBootApplication
public class Cv19FileDownloaderApplication {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        SpringApplication.run(Cv19FileDownloaderApplication.class, args);
    }

}
