package info.alpenglow;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;


public class ListArchiveCrawlerController {

    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "./root";
        int numberOfCrawlers = 16;

        CrawlConfig config = new CrawlConfig();
        config.setMaxDepthOfCrawling(2);
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setMaxDownloadSize(100000000);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        controller.addSeed("http://www.chebucto.ns.ca/Environment/FNSN/MList/");

        controller.start(ListArchiveCrawler.class, numberOfCrawlers);

    }
}