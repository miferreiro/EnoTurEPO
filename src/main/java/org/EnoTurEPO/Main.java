package org.EnoTurEPO;

import com.opencsv.CSVReader;
import org.EnoTurEPO.Reviews.WebExtractor;

import java.io.FileReader;
import java.net.URL;

/**
 * Main class for EnoTurEPO project
 *
 * @author Miguel Ferreiro Díaz
 */
public class Main {

    /**
     * The main method for the running application
     */
    public static void main(String[] args) {

        System.setProperty("webdriver.gecko.driver", "src/main/resources/geckodriver.exe");
        String csvFilePlaces = "src/main/resources/output/outputPlacesCSV.csv";
        String csvFileReviews = "src/main/resources/output/outputReviewsCSV.csv";
        String emoticonsFolder = "src/main/resources/emoticons/";
        String emojisFolder = "src/main/resources/emojis/";
        try{
            WebExtractor webExtractor = new WebExtractor(csvFilePlaces, csvFileReviews, emoticonsFolder, emojisFolder);
            CSVReader csvReader = new CSVReader(new FileReader("src/main/resources/Excel_DatosWebs.csv"), ';', '"',1);
            String[] row;
            while((row = csvReader.readNext()) != null) {
                webExtractor.run(row[0], row[1], new URL(row[2]));
            }
            csvReader.close();
        }catch(Exception e){
            System.err.println(e.getMessage());
        }
    }
}