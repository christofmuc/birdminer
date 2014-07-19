package info.alpenglow;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class PercolateAllDocuments {

    private static Client client;

    static public void percolate(String docId) throws IOException {
        System.err.print("Percolating document " + docId);

        PercolateResponse response = client
                .preparePercolate()
                .setGetRequest(new GetRequest("facebook", "post", docId))
                .setDocumentType("post")
                .execute().actionGet();

        List<String> birds = new LinkedList<>();
        List<String> locations = new LinkedList<>();
        if (response.getMatches().length > 0) {
            for (PercolateResponse.Match m : response.getMatches()) {
                String id = m.getId().string();
                if (id.startsWith("Bird_")) {
                    birds.add(id);
                }
                if (id.startsWith("Location_")) {
                    locations.add(id);
                }
            }
        }

        XContentBuilder builder = jsonBuilder().startObject();

        if (birds.size() > 0) {
            builder.startArray("birds");
            for (String bird : birds)
                builder.value(bird);
            builder.endArray();
        }

        if (locations.size() > 0) {
            builder.startArray("locations");
            for (String location : locations)
                builder.value(location);
            builder.endArray();
        }
        builder.field("percolated", true);
        builder.endObject();

        try {
            client.prepareUpdate("facebook", "post", docId)
                    .setDoc(builder)
                    .execute()
                    .actionGet();
        } catch (Exception e) {
            System.err.println("Update error: " + e.toString());
        }
        System.err.println(": Updated document");
    }

    public static void main(String[] args) throws Exception {
        // Connect to ElasticSearch
        client = TransportClientFactory.createClient("facebook");

        // Now loop over all documents in the index
        SearchResponse response = client.prepareSearch("facebook")
                .setTypes("post")
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setSize(10)
                .setQuery(QueryBuilders.matchAllQuery())
                .execute()
                .actionGet();

        int docs = 0;
        while (true) {
            response = client.prepareSearchScroll(response.getScrollId())
                    .setScroll(new TimeValue(600000)).execute()
                    .actionGet();
            if (response.getHits().getHits().length == 0) {
                break;
            }
            for (SearchHit hit : response.getHits()) {
                // We are only interested in the id of the hit, because we will run the percolator API on it
                docs++;
                percolate(hit.getId());
            }
        }
        System.err.println("Percolated " + docs + " documents");

        // Close ElasticSearch
        client.close();
    }
}
