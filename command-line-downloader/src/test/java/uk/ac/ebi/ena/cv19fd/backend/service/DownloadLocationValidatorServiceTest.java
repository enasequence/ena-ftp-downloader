package uk.ac.ebi.ena.cv19fd.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class DownloadLocationValidatorServiceTest {

    @InjectMocks
    DownloadLocationValidatorService downloadLocationValidatorService;

    private final static String downloadFolderPath = "downloads/";

    @BeforeEach
    public void setup() {
        new File(downloadFolderPath).mkdir();
    }

    @AfterEach
    public void cleanUp() {
        new File(downloadFolderPath).delete();
    }

    @Test
    public void testValidateDownloadLocation() {
        boolean isValid = downloadLocationValidatorService.validateDownloadLocation(downloadFolderPath);

        Assertions.assertTrue(isValid);
    }
}
