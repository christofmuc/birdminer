package info.alpenglow;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.SimpleQueryStringFlag;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.elasticsearch.index.query.QueryBuilders.simpleQueryString;

class CreateLocationNamePercolators {

    private Client client;

    private int idx = 0;

    CreateLocationNamePercolators(Client client) {
        this.client = client;
    }

    private void createPercolator(String locationName) throws IOException {
        System.out.println("Creating percolator for " + locationName);

        SimpleQueryStringBuilder qb;
        qb = simpleQueryString("\\\"" + locationName + "\\\"");
        qb.flags(SimpleQueryStringFlag.PHRASE);

        client.prepareIndex("percolators", ".percolator", "Location_" + idx)
                .setSource(XContentFactory.jsonBuilder().startObject().field("query", qb).field("location", locationName).endObject())
                .setRefresh(true)
                .execute()
                .actionGet();

        idx++;
    }

    private void createLocationNamePercolators(String inputFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String line;
        while ((line = reader.readLine()) != null) {
            createPercolator(line);
        }
    }

    public void createPercolators() throws IOException {
        // Load the file with bird names and create the percolators
        createLocationNamePercolators("input/Hotspots Nova Scotia.txt");
    }
}
