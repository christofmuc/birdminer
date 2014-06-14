package info.alpenglow;

import org.elasticsearch.client.Client;

import java.io.IOException;

public class CreatePercolators {

    public static void main(String[] args) throws IOException {

        Client client = TransportClientFactory.createClient("percolators");

        CreateBirdNamePercolators birdNames = new CreateBirdNamePercolators(client);
        birdNames.createPercolators();

        CreateLocationNamePercolators locationNames = new CreateLocationNamePercolators(client);
        locationNames.createPercolators();

        // Close ElasticSearch
        client.close();
    }
}
