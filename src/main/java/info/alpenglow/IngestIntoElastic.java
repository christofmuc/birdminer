package info.alpenglow;

import com.google.gson.JsonObject;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


class IngestFile extends SimpleFileVisitor<Path> {


    public static final String ELASTIC_INDEX_BIRDING = "birding";

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

        XContentBuilder c = jsonBuilder().startObject()
                .field("doc").startObject()
                .field("text", sb.toString())
                .endObject().endObject();

        PercolateResponse response = IngestIntoElastic.getClient().preparePercolate()
                .setIndices("birding")
                .setDocumentType("sighting")
                .setSource(c).execute().actionGet();

        for (PercolateResponse.Match m : response.getMatches()) {
            System.out.println(m.getId());
        }

        // Store it in ElasticSearch
//        JsonObject message = new JsonObject();
//        message.addProperty("file", file.toString());
//        message.addProperty("message", sb.toString());
//        IndexResponse response = IngestIntoElastic.getClient().prepareIndex("birding", "post")
//                .setSource(message.toString())
//                .execute()
//                .actionGet();

        // Return success
        return FileVisitResult.CONTINUE;
    }

    //TESTING
    private void checkIndexExists(Client client) {
        final GetResponse birdingReq;
        try {
            birdingReq = client.get(new GetRequest(ELASTIC_INDEX_BIRDING)).actionGet();

        } catch (ElasticsearchException e) {
            e.printStackTrace();
            client.admin().indices().create(new CreateIndexRequest(ELASTIC_INDEX_BIRDING));
        }

    }
}

public class IngestIntoElastic {

    private static Client client;

    public static Client getClient() {
        return client;
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        // Connect to ElasticSearch
        client = TransportClientFactory.createClient();

        final URI downloads = IngestIntoElastic.class.getResource("/downloaded").toURI();
        Path path = Paths.get(downloads);

        // Now traverse the folders and ingest all files into the elastic search index
        // Path path = FileSystems.getDefault().getPath("downloaded");

        Files.walkFileTree(path, new IngestFile());

        // Close ElasticSearch
        client.close();
    }
}
