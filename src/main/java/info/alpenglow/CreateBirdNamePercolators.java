package info.alpenglow;

import au.com.bytecode.opencsv.CSVReader;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.FileReader;
import java.io.IOException;

public class CreateBirdNamePercolators {

    private static Client client;

    public static Client getClient() {
        return client;
    }

    private static void createPercolator(String birdName) {
        //TODO: Code goes here
        System.out.println("Creating percolator for " + birdName);
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
        // Connect to ElasticSearch
        client = new TransportClient()
                .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

        // Load the file with bird names and create the percolators
        createBirdNamePercolators("input/LR_USA_List_Flat_20081103.txt");

        // Close ElasticSearch
        getClient().close();

    }
}
