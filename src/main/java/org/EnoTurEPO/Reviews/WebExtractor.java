package org.EnoTurEPO.Reviews;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import org.EnoTurEPO.util.Trio;
import org.bdp4j.util.CSVDatasetWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager of wineries in Google Maps
 *
 * @author Miguel Ferreiro Díaz
 */

public class WebExtractor {

    /**
     * A language detector to guess the language
     */
    private LanguageDetector languageDetector;

    /**
     * A instance of CSVDatasetWriter which manage the CSV of winery information
     */
    private final CSVDatasetWriter outputPlacesCSV;

    /**
     * A instance of CSVDatasetWriter which manage the CSV of winery reviews
     */
    private final CSVDatasetWriter outputReviewsCSV;

    /**
     * The folder name where emoticons file is located
     */
    private final String emoticonsFolder;

    /**
     * The folder name where emojis file is located
     */
    private final String emojisFolder;

    /**
     * A map with the winery information
     */
    private Map<String, Object> placesCSV;

    /**
     * A map with the winery reviews
     */
    private Map<String, Object> reviewsCSV;

    /**
     * A hashmap with the emoji dictionary
     */
    private HashMap<String, Trio<Pattern, String, Double>> emojiDictionary;

    /**
     * A hashmap with the emoticon dictionary
     */
    private HashMap<String, Trio<Pattern, String, Double>> emoticonDictionary;

    /**
     * Constructs a new instance of {@link WebExtractor}
     *
     * @param csvFilePlaces Output file path with winery information
     * @param csvFileReviews Output file path with winery reviews
     * @param emoticonsFolder Path of the folder where the emoticon files containing <emoticon,<polarity, synsetID>> are located
     * @param emojisFolder Path of the folder where the emoji files containing <emoji,<polarity, synsetID>> are located
     * @throws FileNotFoundException if the files do not exist
     */
    public WebExtractor(String csvFilePlaces, String csvFileReviews, String emoticonsFolder, String emojisFolder) throws FileNotFoundException {

        try {
            //load all languages:
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();

            //build language detector:
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();

        } catch (IOException e) {
            System.err.println("Language detector profiles could not be loaded");
        }

        this.outputPlacesCSV = new CSVDatasetWriter(csvFilePlaces);
        this.outputReviewsCSV = new CSVDatasetWriter(csvFileReviews);

        this.emoticonsFolder = emoticonsFolder;
        this.emojisFolder = emojisFolder;

        this.placesCSV = new LinkedHashMap<>();
        this.reviewsCSV = new LinkedHashMap<>();

        this.initializePlacesCSV();
        this.initializeReviewsCSV();
        this.loadEmojiDictionary();
        this.loadEmoticonDictionary();
    }

    /**
     * Initializes the CSV header which contains the winery information
     */
    public void initializePlacesCSV() {

        this.placesCSV.put("winery", 0);
        this.placesCSV.put("D.O.", 0);
        this.placesCSV.put("urlGoogleMaps", 0);
        this.placesCSV.put("longitude", 0);
        this.placesCSV.put("latitude", 0);
        this.placesCSV.put("title", 0);
        this.placesCSV.put("ranking", 0);
        this.placesCSV.put("numReviews", 0);
        this.placesCSV.put("address", 0);
        this.placesCSV.put("schedule", 0);
        this.placesCSV.put("web", 0);
        this.placesCSV.put("telephone", 0);
        this.placesCSV.put("plusCode", 0);
        this.placesCSV.put("stars5", 0);
        this.placesCSV.put("stars4", 0);
        this.placesCSV.put("stars3", 0);
        this.placesCSV.put("stars2", 0);
        this.placesCSV.put("stars1", 0);
        this.placesCSV.put("numPhotos", 0);

        String[] columns = this.placesCSV.keySet().toArray(new String[0]);
        this.outputPlacesCSV.addColumns(columns, this.placesCSV.values().toArray());
    }

    /**
     * Initializes the CSV header which contains the winery reviews
     */
    public void initializeReviewsCSV() {

        this.reviewsCSV.put("title", 0);
        this.reviewsCSV.put("author", 0);
        this.reviewsCSV.put("isLocalGuide", 0);
        this.reviewsCSV.put("numReviewsAuthor", 0);
        this.reviewsCSV.put("rankingReview", 0);
        this.reviewsCSV.put("dateExtractData", 0);
        this.reviewsCSV.put("dateReview", 0);
        this.reviewsCSV.put("textReviewOriginal", 0);
        this.reviewsCSV.put("textReview", 0);
        this.reviewsCSV.put("emojisTextReview", 0);
        this.reviewsCSV.put("emojisPolarityReview", 0);
        this.reviewsCSV.put("emoticonsTextReview", 0);
        this.reviewsCSV.put("emoticonsPolarityReview", 0);
        this.reviewsCSV.put("langTextReview", 0);
        this.reviewsCSV.put("langReliabilityTextReview", 0);
        this.reviewsCSV.put("numPhotoReview", 0);
        this.reviewsCSV.put("likesReview", 0);
        this.reviewsCSV.put("dateAnswer", 0);
        this.reviewsCSV.put("textAnswerOriginal", 0);
        this.reviewsCSV.put("textAnswer", 0);
        this.reviewsCSV.put("emojisTextAnswer", 0);
        this.reviewsCSV.put("emojisPolarityAnswer", 0);
        this.reviewsCSV.put("emoticonsTextAnswer", 0);
        this.reviewsCSV.put("emoticonsPolarityAnswer", 0);
        this.reviewsCSV.put("langTextAnswer", 0);
        this.reviewsCSV.put("langReliabilityTextAnswer", 0);

        String[] columns = this.reviewsCSV.keySet().toArray(new String[0]);
        this.outputReviewsCSV.addColumns(columns,this.reviewsCSV.values().toArray());
    }

    /**
     * Loads the emojis dictionary from a .json file to a HashMap
     *
     * @throws FileNotFoundException if the emoji file does not exist
     */
    public void loadEmojiDictionary() throws FileNotFoundException {

        this.emojiDictionary = new HashMap<>();

        String emojisFilePath = this.emojisFolder + "emojisID.es.json";
        File emojisFile = new File(emojisFilePath);
        InputStream is = new FileInputStream(emojisFile);
        JsonReader rdr = Json.createReader(is);
        JsonObject jsonObject = rdr.readObject();
        rdr.close();

        for (String emoji : jsonObject.keySet()) {
            this.emojiDictionary.put(emoji, new Trio<>(Pattern.compile(Pattern.quote(emoji)), jsonObject.getJsonObject(emoji).getString("synsetID"),
                    jsonObject.getJsonObject(emoji).getJsonNumber("polarity").doubleValue()));

        }
    }

    /**
     * Loads the emoticons dictionary from a .json file to a HashMap
     *
     * @throws FileNotFoundException if the emoticon file does not exist
     */
    public void loadEmoticonDictionary() throws FileNotFoundException {

        this.emoticonDictionary = new HashMap<>();

        String emoticonsFilePath = this.emoticonsFolder + "emoticonsID.es.json";
        File emoticonsFile = new File(emoticonsFilePath);
        InputStream is = new FileInputStream(emoticonsFile);
        JsonReader rdr = Json.createReader(is);
        JsonObject jsonObject = rdr.readObject();
        rdr.close();

        for (String emoticon : jsonObject.keySet()) {
            this.emoticonDictionary.put(emoticon, new Trio<>(Pattern.compile("(\\s|^)" + Pattern.quote(emoticon) + "(\\s|$)"), jsonObject.getJsonObject(emoticon).getString("synsetID"),
                    jsonObject.getJsonObject(emoticon).getJsonNumber("polarity").doubleValue()));
        }
    }

    /**
     * Starts processing all the data from winery
     *
     * @param name Winery name
     * @param origin Winery's designation of origin
     * @param url Google Maps web address where the winery is located
     * @throws InterruptedException if any thread has interrupted the current thread during the execution of Thread.sleep
     */
    public void run(String name, String origin, URL url) throws InterruptedException {

        this.placesCSV = new LinkedHashMap<>();
        this.reviewsCSV = new LinkedHashMap<>();

        System.out.println("--- Begin of data collection from winery " + name + " ---");

        String title, ranking, address, schedule, web, telephone, plusCode;
        int numReviews, numPhotos, stars5, stars4, stars3, stars2, stars1;

        //Coords
        Pattern pattern = Pattern.compile("(/@)(-?[0-9.]+,-?[0-9.]+)");
        Matcher matcher = pattern.matcher(url.toString());
        String coords = null;
        while (matcher.find()) { coords = matcher.group(2); }
        float lng, lat;

        if (coords != null) {
            lng = Float.parseFloat(coords.split(",")[0]);
            lat = Float.parseFloat(coords.split(",")[1]);
        } else {
            System.err.println("The coordinates can not be obtained for :" + name + ". Aborting...");
            return;
        }

        System.out.println("Longitude: " + lng + " Latitude: " + lat);

        FirefoxOptions op = new FirefoxOptions();
        op.addPreference("javascript.enable", true);
        WebDriver driver = new FirefoxDriver(op);
        JavascriptExecutor js;
        Document doc = null, docPhotos = null, docReviews = null, docAuxReviews;

        System.out.println("*** Start reading the winery's page " + name + " ***");

        try {
            driver.get(url.toExternalForm());
            WebDriverWait wait = new WebDriverWait(driver,30);
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("section-hero-header-title-title")));
            Thread.sleep(3000);
            doc = Jsoup.parse(driver.getPageSource()).normalise();
            Thread.sleep(1000);
            js = (JavascriptExecutor) driver;
            js.executeScript("document.getElementById(\"consent-bump\").remove()");
            Thread.sleep(1000);
            if (driver.findElements(By.cssSelector("button[aria-labelledby=\"card-label-Todas\"]")).size() != 0) {
                driver.findElement(By.cssSelector("button[aria-labelledby=\"card-label-Todas\"]")).click();
                Thread.sleep(3000);
                js = (JavascriptExecutor) driver;
                while (driver.findElements(By.className("section-" +
                        "loading")).size() > 0) {
                    js.executeScript("document.getElementsByClassName(\"section-loading\")[0].scrollIntoView()");
                    Thread.sleep(4000);
                }
                docPhotos = Jsoup.parse(driver.getPageSource()).normalise();
                Thread.sleep(3000);
                driver.get(url.toExternalForm());
                Thread.sleep(1000);
                js = (JavascriptExecutor) driver;
                js.executeScript("document.getElementById(\"consent-bump\").remove()");
                Thread.sleep(1000);
            }
            Thread.sleep(3000);
            if (driver.findElements(By.cssSelector("button[jsaction=\"pane.rating.moreReviews\"]")).size() != 0) {
                driver.findElement(By.cssSelector("button[jsaction=\"pane.rating.moreReviews\"]")).click();
                Thread.sleep(3000);
                js = (JavascriptExecutor) driver;
                js.executeScript("var items = document.querySelectorAll('.section-expand-review');for (var i = 0; i < items.length; i++) { items[i].click();}");
                Thread.sleep(3000);
                while (driver.findElements(By.className("section-loading")).size() > 0) {
                    docAuxReviews = Jsoup.parse(driver.getPageSource()).normalise();
                    js.executeScript("document.getElementsByClassName(\"section-loading\")[0].scrollIntoView()");
                    js.executeScript("var items = document.querySelectorAll('.section-expand-review');for (var i = 0; i < items.length; i++) { items[i].click();}");
                    Thread.sleep(4000);
                    docReviews = Jsoup.parse(driver.getPageSource()).normalise();
                    if (docAuxReviews.toString().equals(docReviews.toString())) {
                        break;
                    }
                }
                js.executeScript("var items = document.querySelectorAll('.section-expand-review');for (var i = 0; i < items.length; i++) { items[i].click();}");
                Thread.sleep(3000);
                docReviews = Jsoup.parse(driver.getPageSource()).normalise();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        System.out.println("*** Finish reading the winery's page " + name + " ***");
        title = doc.getElementsByClass("section-hero-header-title-title").first().text().trim();
        System.out.println("Title: " + title);

        ranking = doc.getElementsByClass("section-star-display").text().replace(",",".");
        System.out.println("Ranking: " + ranking);

        if (doc.getElementsByClass("section-rating-term-list").select("button").size() != 0) {
            numReviews = Integer.parseInt(doc.getElementsByClass("section-rating-term-list").select("button").text().trim().replaceAll("[()]",""));
        } else {
            numReviews = 0;
        }
        System.out.println("Number of reviews: " + numReviews);

        address = doc.getElementsByAttributeValue("data-item-id", "address").attr("aria-label").replaceAll("^Dirección: ", "").replaceAll("Province of Ourense", "Ourense").trim();
        System.out.println("Address: " + address);

        schedule = doc.getElementsByClass("section-open-hours-container").attr("aria-label").replaceAll(". Ocultar el horario de la semana$", "");
        System.out.println("Schedule: " + schedule);

        web = doc.getElementsByAttributeValue("data-item-id", "authority").attr("aria-label").replaceAll("^Sitio web: ", "").trim();
        System.out.println("Web: " + web);

        telephone = doc.getElementsByAttributeValueContaining("data-item-id", "phone:tel:").attr("aria-label").replaceAll("^Teléfono: ", "").trim();
        System.out.println("Telephone: " + telephone);

        plusCode = doc.getElementsByAttributeValue("data-item-id", "oloc").first().getElementsByClass("ugiz4pqJLAG__primary-text").first().text();
        System.out.println("Plus code: " + plusCode);

        if (!doc.getElementsByAttributeValueContaining("aria-label", "5 estrellas,").attr("aria-label").equals("")) {
            stars5 = Integer.parseInt(doc.getElementsByAttributeValueContaining("aria-label", "5 estrellas,").attr("aria-label").split(",")[1].trim().split(" ")[0].trim().replace(" ", ""));
        } else {
            stars5 = -1;
        }
        if (!doc.getElementsByAttributeValueContaining("aria-label", "4 estrellas,").attr("aria-label").equals("")) {
            stars4 = Integer.parseInt(doc.getElementsByAttributeValueContaining("aria-label", "4 estrellas,").attr("aria-label").split(",")[1].trim().split(" ")[0].trim().replace(" ", ""));
        } else {
            stars4 = -1;
        }
        if (!doc.getElementsByAttributeValueContaining("aria-label", "3 estrellas,").attr("aria-label").equals("")) {
            stars3 = Integer.parseInt(doc.getElementsByAttributeValueContaining("aria-label", "3 estrellas,").attr("aria-label").split(",")[1].trim().split(" ")[0].trim().replace(" ", ""));
        } else {
            stars3 = -1;
        }
        if (!doc.getElementsByAttributeValueContaining("aria-label", "2 estrellas,").attr("aria-label").equals("")) {
            stars2 = Integer.parseInt(doc.getElementsByAttributeValueContaining("aria-label", "2 estrellas,").attr("aria-label").split(",")[1].trim().split(" ")[0].trim().replace(" ", ""));
        } else {
            stars2 = -1;
        }
        if (!doc.getElementsByAttributeValueContaining("aria-label", "1 estrellas,").attr("aria-label").equals("")) {
            stars1 = Integer.parseInt(doc.getElementsByAttributeValueContaining("aria-label", "1 estrellas,").attr("aria-label").split(",")[1].trim().split(" ")[0].trim().replace(" ", ""));
        } else {
            stars1 = -1;
        }
        System.out.println("Stars5: " + stars5 + "\nStars4: " + stars4 + "\nStars3: " + stars3 + "\nStars2: " + stars2 + "\nStars1: " + stars1);

        if (docPhotos != null) {
            numPhotos = Integer.parseInt(docPhotos.getElementsByClass("gallery-cell").last().attr("data-photo-index")) + 1;
        } else {
            Pattern pattern2 = Pattern.compile("([0-9]+) fotos?");
            Matcher matcher2 = pattern2.matcher(doc.toString());
            if (matcher2.find()) {
                numPhotos = Integer.parseInt(matcher2.group(1));
            } else{
                numPhotos = 0;
            }
        }
        System.out.println("NumPhotos: " + numPhotos);

        this.placesCSV.put("winery", name);
        this.placesCSV.put("D.O.", origin);
        this.placesCSV.put("urlGoogleMaps", url);
        this.placesCSV.put("longitude", lng);
        this.placesCSV.put("latitude", lat);
        this.placesCSV.put("title", title);
        if (ranking.equals("")) {
            this.placesCSV.put("ranking", "UND");
        } else {
            this.placesCSV.put("ranking", ranking);
        }
        this.placesCSV.put("numReviews", numReviews);
        this.placesCSV.put("address", address);
        this.placesCSV.put("schedule", schedule);
        this.placesCSV.put("web", web);
        this.placesCSV.put("telephone", telephone);
        this.placesCSV.put("plusCode", plusCode);
        this.placesCSV.put("stars5", stars5);
        this.placesCSV.put("stars4", stars4);
        this.placesCSV.put("stars3", stars3);
        this.placesCSV.put("stars2", stars2);
        this.placesCSV.put("stars1", stars1);
        this.placesCSV.put("numPhotos", numPhotos);

        System.out.println("*** Saving the data of the winery " + title + " ***");
        this.outputPlacesCSV.addRow(this.placesCSV.values().toArray());
        this.outputPlacesCSV.flushAndClose();
        System.out.println("*** Saved the data of the winery " + title + " ***");

        if (docReviews != null) {

            String author;
            boolean isLocalGuide;
            int numReviewsAuthor;
            String rankingReview, dateReview, textReview, langTextReview;
            double langReliabilityTextReview, langReliabilityTextAnswer;
            int numPhotoReview, likesReview;
            String dateAnswer, textAnswer, langTextAnswer;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            String dateExtractData = dateFormat.format(new Date())  ;

            System.out.println("*** Start reading the reviews of winery's page " + title + " ***");

            int contReview = 1;
            for(Element e : docReviews.getElementsByClass("section-review")) {

                System.out.println("**************************************************");
                System.out.println("Num review: " + contReview);

                author = e.getElementsByClass("section-review-title").text();
                System.out.println("Author: " + author);

                String subtitle = e.getElementsByClass("section-review-subtitle").text().trim();
                if (subtitle.contains("・")) {
                    String[] subtitleSplit = subtitle.split("・");
                    isLocalGuide = true;
                    numReviewsAuthor = Integer.parseInt(subtitleSplit[1].split(" ")[0].trim().replace(".", ""));
                } else {
                    if (subtitle.matches("Local Guide [0-9]+ reseñas?")) {
                        isLocalGuide = false;
                        numReviewsAuthor = Integer.parseInt(subtitle.split(" ")[2].replace(".", ""));
                    } else {
                        isLocalGuide = !subtitle.trim().equals("");
                        numReviewsAuthor = 0;
                    }
                }
                System.out.println("IsLocalGuide: " + isLocalGuide);
                System.out.println("NumReviewsAuthor: " + numReviewsAuthor);

                if (e.getElementsByClass("section-review-numerical-rating") != null && e.getElementsByClass("section-review-numerical-rating").size() > 0) {
                    rankingReview = String.valueOf(e.getElementsByClass("section-review-numerical-rating").text().trim().charAt(0));
                } else {
                    rankingReview = e.getElementsByClass("section-review-stars").attr("aria-label").trim().split(" ")[0];
                }
                System.out.println("RankingReview: " + rankingReview);

                if (e.getElementsByClass("section-review-publish-date-and-source") != null && e.getElementsByClass("section-review-publish-date-and-source").size() > 0) {
                    dateReview = e.getElementsByClass("section-review-publish-date-and-source").first().text().replaceAll(" en Google", "");
                } else {
                    dateReview = e.getElementsByClass("section-review-publish-date").text();
                }
                System.out.println("DateReview: " + dateReview);

                numPhotoReview = e.getElementsByAttributeValue("aria-label", "Foto").size();
                System.out.println("NumPhotoReview: " + numPhotoReview);

                dateAnswer = e.getElementsByClass("section-review-owner-response").select(".section-review-owner-response-subtitle").text();
                System.out.println("DateAnswer: " + dateAnswer);

                if (e.getElementsByClass("section-review-thumbs-up-count").text().trim().equals("")) {
                    likesReview = 0;
                } else {
                    likesReview = Integer.parseInt(e.getElementsByClass("section-review-thumbs-up-count").text().trim());
                }
                System.out.println("LikesReview: " + likesReview);

                textReview = e.getElementsByClass("section-review-text").text().trim();
                String[] splitTextReview = textReview.split("\\(Original\\)");
                String textReviewOriginal;
                if (splitTextReview.length > 1) {
                    textReview = splitTextReview[0].replaceAll("\\(Traducido por Google\\)", "").trim();
                    textReviewOriginal = splitTextReview[1].trim();
                } else {
                    textReviewOriginal = textReview.trim();
                }
                System.out.println("TextReview: " + textReview);
                System.out.println("TextReviewOriginal: " + textReviewOriginal);

                List<DetectedLanguage> langList = this.languageDetector.getProbabilities(new StringBuffer(textReviewOriginal));

                LdLocale bestlang = null;
                double prob = 0.0;
                for (DetectedLanguage lang : langList) {
                    if (lang.getProbability() > prob) {
                        bestlang = lang.getLocale();
                        prob = lang.getProbability();
                    }
                }

                if (bestlang != null) {
                    langTextReview = bestlang.getLanguage().toUpperCase();
                    langReliabilityTextReview = prob;
                } else {
                    langTextReview = "UND";
                    langReliabilityTextReview = -1.0;
                }

                System.out.println("LangTextReview: " + langTextReview);
                System.out.println("LangReliabilityTextReview: " + langReliabilityTextReview);

                textAnswer = e.getElementsByClass("section-review-owner-response").select(".section-review-text").text().trim();
                String[] splitTextAnswer = textAnswer.split("\\(Original\\) ");
                String textAnswerOriginal;
                if (splitTextAnswer.length > 1) {
                    textAnswer = splitTextAnswer[0].replaceAll("\\(Traducido por Google\\) ", "").trim();
                    textAnswerOriginal = splitTextAnswer[1].trim();
                } else {
                    textAnswerOriginal = textAnswer.trim();
                }
                System.out.println("TextAnswer: " + textAnswer);
                System.out.println("TextAnswerOriginal: " + textAnswerOriginal);
                langList = this.languageDetector.getProbabilities(new StringBuffer(textAnswerOriginal));

                bestlang = null;
                prob = 0.0;
                for (DetectedLanguage lang : langList) {
                    if (lang.getProbability() > prob) {
                        bestlang = lang.getLocale();
                        prob = lang.getProbability();
                    }
                }

                if (bestlang != null) {
                    langTextAnswer = bestlang.getLanguage().toUpperCase();
                    langReliabilityTextAnswer = prob;
                } else {
                    langTextAnswer = "UND";
                    langReliabilityTextAnswer = -1.0;
                }
                System.out.println("LangTextAnswer: " + langTextAnswer);
                System.out.println("LangReliabilityTextAnswer: " + langReliabilityTextAnswer);

                System.out.println("**************************************************");

                this.reviewsCSV.put("title", title);
                this.reviewsCSV.put("author", author);
                if (isLocalGuide) {
                    this.reviewsCSV.put("isLocalGuide", 1);
                } else {
                    this.reviewsCSV.put("isLocalGuide", 0);
                }
                this.reviewsCSV.put("numReviewsAuthor", numReviewsAuthor);
                this.reviewsCSV.put("rankingReview", rankingReview);
                this.reviewsCSV.put("dateExtractData", dateExtractData);
                this.reviewsCSV.put("dateReview", dateReview);
                this.reviewsCSV.put("textReviewOriginal", textReviewOriginal);

                Trio<String, String, Double> outputEmojisReview = this.manageEmojis(textReview);
                textReview = outputEmojisReview.getObj1();
                String emojisTextReview = outputEmojisReview.getObj2();
                Double emojisPolarityReview = outputEmojisReview.getObj3();

                Trio<String, String, Double> outputEmoticonsReview = this.manageEmoticons(textReview);
                textReview = outputEmoticonsReview.getObj1();
                String emoticonsTextReview = outputEmoticonsReview.getObj2();
                Double emoticonsPolarityReview = outputEmoticonsReview.getObj3();

                this.reviewsCSV.put("textReview", textReview);
                this.reviewsCSV.put("emojisTextReview", emojisTextReview);
                this.reviewsCSV.put("emojisPolarityReview", emojisPolarityReview);

                this.reviewsCSV.put("emoticonsTextReview", emoticonsTextReview);
                this.reviewsCSV.put("emoticonsPolarityReview", emoticonsPolarityReview);

                this.reviewsCSV.put("langTextReview", langTextReview);
                this.reviewsCSV.put("langReliabilityTextReview", langReliabilityTextReview);
                this.reviewsCSV.put("numPhotoReview", numPhotoReview);
                this.reviewsCSV.put("likesReview", likesReview);

                this.reviewsCSV.put("dateAnswer", dateAnswer);
                this.reviewsCSV.put("textAnswerOriginal", textAnswerOriginal);

                Trio<String, String, Double> outputEmojisAnswer = this.manageEmojis(textAnswer);
                textAnswer = outputEmojisAnswer.getObj1();
                String emojisTextAnswer = outputEmojisAnswer.getObj2();
                Double emojiPolarityAnswer = outputEmojisAnswer.getObj3();

                Trio<String, String, Double> outputEmoticonsAnswer = this.manageEmoticons(textAnswer);
                textAnswer = outputEmoticonsAnswer.getObj1();
                String emoticonsTextAnswer = outputEmoticonsAnswer.getObj2();
                Double emoticonsPolarityAnswer = outputEmoticonsAnswer.getObj3();

                this.reviewsCSV.put("textAnswer", textAnswer);

                this.reviewsCSV.put("emojisTextAnswer", emojisTextAnswer);
                this.reviewsCSV.put("emojisPolarityAnswer", emojiPolarityAnswer);

                this.reviewsCSV.put("emoticonsTextAnswer", emoticonsTextAnswer);
                this.reviewsCSV.put("emoticonsPolarityAnswer", emoticonsPolarityAnswer);

                this.reviewsCSV.put("langTextAnswer", langTextAnswer);
                this.reviewsCSV.put("langReliabilityTextAnswer", langReliabilityTextAnswer);

                System.out.println("*** Saving the " + contReview + " review of the winery " + title + " ***");
                this.outputReviewsCSV.addRow(this.reviewsCSV.values().toArray());
                this.outputReviewsCSV.flushAndClose();
                System.out.println("*** Saved the " + contReview + " review of the winery " + title + " ***");

                contReview++;
            }
            Thread.sleep(1000);
        } else {
            System.out.println("*** There are not reviews of the winery " + title + " ***");
        }
        System.out.println("--- End of data collection from winery " + name + " ---");
    }

    /**
     * Detects and replaces the emojis found by their textual representation and calculates their polarity
     *
     * @param text Text to be processed
     * @return Trio structure with the text modified, the emojis found and the polarity calculated
     */
    public Trio<String, String, Double> manageEmojis(String text) {

        String value = "";
        StringBuffer sb = new StringBuffer(text);
        int numEmojis = 0;
        double score = 0;
        for (String emoji: this.emojiDictionary.keySet()) {
            Pattern pat = this.emojiDictionary.get(emoji).getObj1();
            Matcher match = pat.matcher(sb);
            int last = 0;
            while (match.find(last)) {
                last = match.start(0) + 1;
                // Now replaces emoji pattern by its meaning
                score += this.emojiDictionary.get(emoji).getObj3();
                numEmojis++;
                value += emoji;
                sb = sb.replace(match.start(0),match.end(0)," " + this.emojiDictionary.get(emoji).getObj2() + " ");
            }
        }
        //Calculate arithmetic mean and store in a property
        Double mean = score / (new Double(numEmojis));
        if (Double.isNaN(mean)) {
            mean = 0.0;
        }
        return  new Trio<>(sb.toString().trim(), value, mean) ;
    }

    /**
     * Detects and replaces the emoticons found by their textual representation and calculates their polarity
     *
     * @param text Text to be processed
     * @return Trio structure with the text modified, the emoticons found and the polarity calculated
     */
    public Trio<String, String, Double> manageEmoticons(String text) {

        String value = "";
        StringBuffer sb = new StringBuffer(text);
        int numEmoticons = 0;
        double score = 0;
        for (String emoticon: this.emoticonDictionary.keySet()) {
            Pattern pat = this.emoticonDictionary.get(emoticon).getObj1();
            Matcher match = pat.matcher(sb);
            int last = 0;
            while (match.find(last)) {
                last = match.start(0) + 1;
                // Now replaces emoji pattern by its meaning
                score += this.emoticonDictionary.get(emoticon).getObj3();
                numEmoticons++;
                value += emoticon;
                sb = sb.replace(match.start(0),match.end(0)," " + this.emoticonDictionary.get(emoticon).getObj2() + " ");
            }
        }
        //Calculate arithmetic mean and store in a property
        Double mean = score / (new Double(numEmoticons));
        if (Double.isNaN(mean)) {
            mean = 0.0;
        }
        return  new Trio<>(sb.toString().trim(), value, mean) ;
    }
}