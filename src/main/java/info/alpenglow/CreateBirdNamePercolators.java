package info.alpenglow;

import au.com.bytecode.opencsv.CSVReader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class CreateBirdNamePercolators {

    private static Client client;

    public static Client getClient() {
        return client;
    }

    private static void createPercolator(String birdName) throws IOException {
        //TODO: Code goes here
        System.out.println("Creating percolator for " + birdName);

        JsonArray fuzzymatches = new JsonArray();

        String[] nameSegs = birdName.split(" ");
        for (String seg : nameSegs) {
            JsonObject val = new JsonObject();
            val.addProperty("value", seg);
            JsonObject fuz = new JsonObject();
            fuz.add("text", val);
            fuzzymatches.add(fuz);
        }

        JsonObject must = new JsonObject();
        must.add("must", fuzzymatches);
        JsonObject bool = new JsonObject();
        bool.add("bool",must);
        JsonObject query = new JsonObject();
        query.add("query", bool);

        QueryBuilder qb = QueryBuilders.termQuery("fuzzy","test");

        IndexResponse response = getClient().prepareIndex("birding", ".percolator", birdName)
                .setSource(XContentFactory.jsonBuilder().startObject().field("query",qb).endObject())
                .setRefresh(true)
                .execute()
                .actionGet();
    }

    private static void createBirdNamePercolators(String inputFile) throws IOException {
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

    public static void main(String[] args) throws IOException {
        // Load properties
        Properties prop = new Properties();
        prop.load(new FileInputStream("config.properties"));

        // Connect to ElasticSearch
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", prop.getProperty("cluster.name")).build();
        client = new TransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(prop.getProperty("server.name"), 9300));

        // Load the file with bird names and create the percolators
        createBirdNamePercolators("input/LR_USA_List_Flat_20081103.txt");

        // Close ElasticSearch
        getClient().close();

    }
}
