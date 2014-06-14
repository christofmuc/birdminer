package info.alpenglow;

import com.google.gson.JsonObject;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.xmlbeans.impl.common.ReaderInputStream;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


class IngestFile extends SimpleFileVisitor<Path> {


    public static final String ELASTIC_INDEX_BIRDING = "birding";

    public void extractContent(Path file) throws IOException, TikaException, SAXException {
        InputStream input = new FileInputStream(file.toFile());
        ContentHandler textHandler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();

        parser.parse(input, textHandler, metadata, context);

        // Make stream from content
        StringReader reader = new StringReader(textHandler.toString());
        InputStream input2 = new ReaderInputStream(reader, "UTF8");

        ContentHandler textHandler2 = new BodyContentHandler();
        Metadata metadata2 = new Metadata();
        parser.parse(input2, textHandler2, metadata2, context);


        System.out.println("Title:" + metadata2.get(Metadata.TITLE));
        System.out.println("Type:" + metadata2.get(Metadata.CONTENT_TYPE));
        System.out.println("Body:" + textHandler2.toString());
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        System.out.println("Reading file " + file.toString());

        // Test Tika
        try {
            extractContent(file);
        } catch (TikaException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

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
        IndexResponse response2 = IngestIntoElastic.getClient().prepareIndex("all_documents", "post")
                .setSource(message.toString())
                .execute()
                .actionGet();


        // And run the percolators
        XContentBuilder c = jsonBuilder().startObject()
                .field("doc").startObject()
                .field("text", sb.toString())
                .endObject().endObject();

        PercolateResponse response = IngestIntoElastic.getClient().preparePercolate()
                .setIndices(ELASTIC_INDEX_BIRDING)
                .setDocumentType("sighting")
                .setSource(c).execute().actionGet();

        XContentBuilder builder = jsonBuilder().startObject().field("file", file.toString())
                .field("text", sb.toString()).startArray("percolators");

        if (response.getMatches().length == 0) {
            return FileVisitResult.CONTINUE;
        }

        boolean hasBird = false;
        boolean hasLocation = false;
        for (PercolateResponse.Match m : response.getMatches()) {
            String id = m.getId().string();
            if (id.startsWith("Bird_")) {
                hasBird = true;
            }
            if (id.startsWith("Location_")) {
                hasLocation = true;
            }
            System.out.println(id);
            builder.value(id);
        }

        builder.endArray();
        builder.endObject();

        if (hasBird && hasLocation) {
            IngestIntoElastic.getClient().prepareIndex("result", "bird_candidate")
                    .setSource(builder).execute().actionGet();
        } else {
            System.out.println("Did not match both Bird and Location");
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
