package info.alpenglow;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.SimpleQueryStringFlag;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.elasticsearch.index.query.QueryBuilders.simpleQueryString;

public class CreateLocationNamePercolators {

    private static Client client;

    private static int idx = 0;

    public static Client getClient() {
        return client;
    }

    private static void createPercolator(String locationName) throws IOException {
        System.out.println("Creating percolator for " + locationName);

        SimpleQueryStringBuilder qb;
        qb = simpleQueryString("\\\"" + locationName + "\\\"");
        qb.flags(SimpleQueryStringFlag.PHRASE);

        IndexResponse response = getClient().prepareIndex("birding", ".percolator", "Location_" + idx)
                .setSource(XContentFactory.jsonBuilder().startObject().field("query", qb).field("location", locationName).endObject())
                .setRefresh(true)
                .execute()
                .actionGet();

        idx++;

    }

    private static void createLocationNamePercolators(String inputFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String line;
        while ((line = reader.readLine()) != null) {
            createPercolator(line);
        }
    }

    public static void main(String[] args) throws IOException {
        // Connect to ElasticSearch
        client = TransportClientFactory.createClient();

        // Load the file with bird names and create the percolators
        createLocationNamePercolators("input/Hotspots Nova Scotia.txt");

        // Close ElasticSearch
        getClient().close();
    }
}
