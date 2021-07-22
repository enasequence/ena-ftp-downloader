package uk.ac.ebi.ena.cv19fd.app.constants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Constants {
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmm");
    public static final String enterMessage = " enter ";
    public static final String forMessage = "For ";
    public static final String toMessage = "To ";
    public static final String backMessage = "To go back enter b";

    public static final String SEMICOLON = ";";
    public static final int CHUNK_SIZE = 1000;
    public static final int EXECUTOR_THREAD_COUNT = 5;
    public static final String HTTP = "http://";
    public static final String FTP = "ftp://";
    public static final int TOTAL_RETRIES = 10;

    public static final String sraExperimentPattern = "^[ESDR]RX[0-9]{6,}$";
    public static final String sraSamplePattern = "^[0-9]+$";
    public static final String projectPattern = "^(PRJ[A-Z]{2}[0-9]+)$";
    public static final String emblPattern = "^([A-Z]{1,2}[0-9]{5,6}(.[0-9]{1,2})?)|([a-zA-Z]{4}[0-9]{2}S[0-9]{6,8}(.[0-9]{1,2})?)$";

    public static final String binFolder = "bin";
    public static final String etcFolder = "etc";
    public static final String ascpFileName = "ascp";
    public static final String asperaWebFile = "asperaweb_id_dsa.openssh";


    public static final String FTP_SRA_SERVER = "ftp.sra.ebi.ac.uk";
}
