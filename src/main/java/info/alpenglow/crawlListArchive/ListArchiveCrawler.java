package info.alpenglow.crawlListArchive;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class ListArchiveCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g"
            + "|png|tiff?|mid|mp2|mp3|mp4"
            + "|wav|avi|mov|mpeg|ram|m4v|pdf"
            + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");


    @Override
    public boolean shouldVisit(WebURL url) {
        // The root needs always be visited
        if (url.getURL().toLowerCase().endsWith("MList/".toLowerCase())) {
            return true;
        }
        // If the file exists, not necessarily...
        File file = new File("root/downloaded/" + url.getURL().substring(50));
        if (file.exists() && !file.isDirectory()) {
            System.out.println("File " + file.getAbsolutePath() + " already exists");
            return false;
        }

        String href = url.getURL().toLowerCase();
        return href.startsWith("http://www.chebucto.ns.ca/Environment/FNSN/MList/".toLowerCase());
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        System.out.println("URL: " + url);

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            List<WebURL> links = htmlParseData.getOutgoingUrls();

            /*System.out.println("Text length: " + text.length());
            System.out.println("Html length: " + html.length());
            System.out.println("Number of outgoing links: " + links.size());
            System.out.println("Message" + text);*/

            if (!url.toLowerCase().endsWith("MList/".toLowerCase())) {
                // Store this document on the local disk!
                File file = new File("root/downloaded/" + url.substring(50));
                new File(file.getParent()).mkdirs();
                try {
                    FileWriter fw = new FileWriter(file);
                    fw.write(htmlParseData.getHtml());
                    fw.close();
                } catch (IOException e) {
                    System.err.println("IOException writing file " + file.getAbsolutePath() + " is " + e.getMessage());
                }
            }
        }
    }
}
