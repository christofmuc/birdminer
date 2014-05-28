package info.alpenglow;

import com.google.gson.JsonObject;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


class Test extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        System.out.println("Reading file " + file.toString());

        // Read the file
        FileReader reader = new FileReader(file.toFile());
        BufferedReader br = new BufferedReader(reader);
        StringBuffer sb = new StringBuffer();
        String line = null;
        do {
            line = br.readLine();
            if (line != null) {
                sb.append(line);
            }
        } while (line != null);

        // Store it in ElasticSearch
        JsonObject message = new JsonObject();
        message.addProperty("file", file.toString());
        message.addProperty("message", sb.toString());
        IndexResponse response = IngestIntoElastic.getClient().prepareIndex("birding", "post")
                .setSource(message.toString())
                .execute()
                .actionGet();

        // Return success
        return FileVisitResult.CONTINUE;
    }
}

public class CreateBirdQueries {

    private static Client client;

    public static Client getClient() {
        return client;
    }

    public static void main(String[] args) throws IOException {
        // Connect to ElasticSearch
        client = new TransportClient()
                .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

        // Loop over all Bird Names and create phrase Query

        // Close ElasticSearch
        client.close();
    }
}
