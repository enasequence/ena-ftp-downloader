import org.junit.Test;

/**
 * Created by suranj on 03/08/2016.
 */
public class Tester {

    @Test
    public void testRegex() {
        String s = "    <jfx:javafx-desc  width=\"500\" height=\"400\" main-class=\"uk.ac.ebi.ena.ftp.gui.Main\" name=\"ENA File Downloader\" >\n" +
                "        <fx:param name=\"accession\" value=\"$accession\"/>\n" +
                "        <fx:argument>$accession</fx:argument>\n" +
                "    <jfx:javafx-desc>\n";

        System.out.println(s.replaceAll("\\$accession", "xxxx"));

    }
}
