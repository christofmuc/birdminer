package info.alpenglow;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CreateLocationNamePercolators {

    private static Client client;

    public static Client getClient() {
        return client;
    }

    private static void createPercolator(String locationName) {
        //TODO: Code goes here
        System.out.println("Creating percolator for " + locationName);
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
