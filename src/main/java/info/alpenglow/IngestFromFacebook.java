package info.alpenglow;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class IngestFromFacebook {

    private static Client client;

    private static void storeInElastic(String id, String url, String text) throws IOException {
        // Store it in ElasticSearch
        XContentBuilder builder = jsonBuilder().startObject();
        builder.field("id", id);
        builder.field("url", url);
        builder.field("message", text);
        builder.endObject();

        try {
            client.prepareIndex("input", "post")
                    .setSource(builder.string())
                    .execute()
                    .actionGet();
        } catch (ActionRequestValidationException ex) {
            System.err.println("Validate error with object " + builder.string());
        }
    }

    private static void outputPosts(JsonObject post) throws IOException {
        String id;
        String url;
        StringBuilder messageText = new StringBuilder();

        try {
            id = post.getString("id");
            url = post.getString("link");
            messageText.append(post.getString("message") + '\n');
            if (post.has("comments")) {
                JsonArray comments = post.getJsonObject("comments").getJsonArray("data");
                for (int j = 0; j < comments.length(); j++) {
                    try {
                        JsonObject comment = comments.getJsonObject(j);
                        messageText.append(comment.getString("message") + '\n');
                    } catch (JsonException ex) {
                        System.err.println("Json comment not complete: " + comments.toString());
                    }
                }
            }
            storeInElastic(id, url, messageText.toString());
        } catch (JsonException ex) {
            System.err.println("Json message not complete: " + post.toString());
        }
    }

    public static void main(final String args[]) throws IOException {
        System.out.println("Starting IngestFromFacebook, connecting to Facebook!");

        // Need to load properties first
        Properties prop = new Properties();
        prop.load(new FileInputStream("properties/config.properties"));

        FacebookClient facebookClient = new DefaultFacebookClient(prop.getProperty("facebook.access.token"));


        // Connect to ElasticSearch
        client = TransportClientFactory.createClient("input");

        // Fetch Birding Group and start reading posts
        JsonObject group = facebookClient.fetchObject("114204608605113", JsonObject.class);
        Connection<JsonObject> feed = facebookClient.fetchConnection(group.get("id") + "/feed", JsonObject.class);

        int i = 0;
        for (List<JsonObject> feedPage : feed) {
            for (JsonObject post : feedPage) {
                System.out.println("Post " + i++);
                outputPosts(post);
            }
        }
    }
}
