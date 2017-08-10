import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by suranj on 03/08/2016.
 */
public class Tester {

    @Test
    public void testRegex() {
        String s = "    <jfx:javafx-desc  width=\"500\" height=\"400\" main-class=\"uk.ac.ebi.ena.downloader.gui.Main\" name=\"ENA FTP Downloader\" >\n" +
                "        <fx:param name=\"accession\" value=\"$accession\"/>\n" +
                "        <fx:argument>$accession</fx:argument>\n" +
                "    <jfx:javafx-desc>\n";

//        log.debug(s.replaceAll("\\$accession", "xxxx"));

    }

    @Test
    public void testAspera() throws IOException {
        String cmd = "C:/Program Files (x86)/Aspera/Aspera Connect/bin/ascp -QT -l 300m -i C:/devtools/aspera-cli/etc/asperaweb_id_dsa.openssh -P 33001 era-fasp@fasp.sra.ebi.ac.uk:/vol1/fastq/DRR000/DRR000001/DRR000001_1.fastq.gz c:/enaBrowser/";

        Process process = Runtime.getRuntime().exec(cmd);

        // Get input streams
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Read command standard output
        String s;
        System.out.println("Standard output: ");
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // Read command errors
        System.out.println("Standard error: ");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

    }
}
