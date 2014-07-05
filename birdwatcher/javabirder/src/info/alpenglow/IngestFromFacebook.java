package info.alpenglow;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class IngestFromFacebook {

    private static void outputPosts(FileWriter out, JsonObject post) throws IOException {
        String id;
        String url;
        StringBuilder messageText = new StringBuilder();

        try {
            id = post.getString("id");
            url = post.getString("link");
            messageText.append(post.getString("message")).append(' ');
            if (post.has("comments")) {
                JsonArray comments = post.getJsonObject("comments").getJsonArray("data");
                for (int j = 0; j < comments.length(); j++) {
                    try {
                        JsonObject comment = comments.getJsonObject(j);
                        messageText.append(comment.getString("message")).append(' ');
                    } catch (JsonException ex) {
                        System.err.println("Json comment not complete: " + comments.toString());
                    }
                }
            }
            out.write(messageText.toString() + '\n');
        } catch (JsonException ex) {
            System.err.println("Json message not complete: " + post.toString());
        }
    }

    public static void main(final String args[]) throws IOException {
        //System.err.println("Starting IngestFromFacebook, connecting to Facebook!");
        // Open Output File
        FileWriter output = new FileWriter("facebookPosts.txt", true);

        FacebookClient facebookClient = new DefaultFacebookClient("CAACEdEose0cBAMh2yEZC6VncZAJVdEAetmeJ5WByauxH55cnK4E2qEfbKeeWubQl0kw2IdRr0mCXNDfc23ohHdAgXRbsRVVZBxhGr9qZB8drNBlnfdeLAQ5ZCAh1z8d23lkPzJT0TfvJsNohZASD8kpQtdPG0H0MGZAHZCWvZBMTus4FCKDm9dEhdYLNKABNt9lgJc42KZBRjNUQZDZD");

        // Fetch Birding Group and start reading posts
        JsonObject group = facebookClient.fetchObject("114204608605113", JsonObject.class);
        Connection<JsonObject> feed = facebookClient.fetchConnection(group.get("id") + "/feed", JsonObject.class);

        int i = 0;
        for (List<JsonObject> feedPage : feed) {
            for (JsonObject post : feedPage) {
                i++;
                //System.err.println("Post " + i++);
                //System.out.println(post.toString());
                //outputPosts(output, post);
                output.write(post.toString() + "\r\n");
            }
            if (i >= 50) {
                output.close();
                System.exit(0);
            }
        }
    }
}
