package info.alpenglow;

import au.com.bytecode.opencsv.CSVReader;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.SimpleQueryStringFlag;

import java.io.FileReader;
import java.io.IOException;

import static org.elasticsearch.index.query.QueryBuilders.simpleQueryString;

class CreateBirdNamePercolators {

    private int idx = 0;
    private Client client;

    public CreateBirdNamePercolators(Client client) {
        this.client = client;
    }

    private void createPercolator(String birdName) throws IOException {
        System.out.println("Creating percolator for " + birdName);

        SimpleQueryStringBuilder qb;
        qb = simpleQueryString("\\\"" + birdName + "\\\"");
        qb.flags(SimpleQueryStringFlag.PHRASE);

        client.prepareIndex("percolators", ".percolator", "Bird_" + idx)
                .setSource(XContentFactory.jsonBuilder().startObject().field("query", qb).field("bird", birdName).endObject())
                .setRefresh(true)
                .execute()
                .actionGet();

        idx++;
    }

    private void createBirdNamePercolators(String inputFile) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(inputFile), '\t');
        String[] elements = reader.readNext();
        if (!elements[0].equals("[North American Birds]")) {
            // Ignore
            System.out.println("This is not the correct file!");
            return;
        }
        while ((elements = reader.readNext()) != null) {
            // The Bird Names file has a specific multi-line format...
            if (elements.length == 2) {
                createPercolator(elements[1]);
            }
        }
    }

    public void createPercolators() throws IOException {
        // Load the file with bird names and create the percolators
        createBirdNamePercolators("input/LR_USA_List_Flat_20081103.txt");
    }
}
