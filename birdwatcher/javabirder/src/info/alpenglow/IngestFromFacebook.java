package info.alpenglow;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;
import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class IngestFromFacebook {

    // Connect to fluentd
    //private static FluentLogger LOG = FluentLogger.getLogger("facebook");

    private static String outputPosts(JsonObject post) {
        String id;
        String url;
        StringBuilder messageText = new StringBuilder();

        try {
            //  id = post.getString("id");
            //url = post.getString("link");
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
            return messageText.toString();
        } catch (JsonException ex) {
            System.err.println("Json message not complete: " + post.toString());
        }
        return "";
    }

    private static RecordManager recMan;

    static {
        try {
            recMan = RecordManagerFactory.createRecordManager("./facebookPostsRead");
        } catch (IOException e) {
            System.err.println("Can't initialize file ./facebookPostsRead");
            System.err.println(e);
            System.exit(-1);
        }
    }

    public static void main(final String args[]) throws IOException {
        //System.err.println("Starting IngestFromFacebook, connecting to Facebook!");
        // Open Output File
        //FileWriter output = new FileWriter("facebookPosts.txt", true);

        // 60 day valid token, don't commit
        // https://stackoverflow.com/questions/9067947/facebook-access-token-server-side-vs-client-side-flows
        // https://graph.facebook.com/oauth/access_token?client_id=1449935445258575&client_secret=fbf29692ebc4d089f7736a6dd56c1a87&grant_type=fb_exchange_token&fb_exchange_token=CAAUmtWcpcU8BAMsxtV99pG93qGhEpsifgjZCwAiMi27tDiEIMMc5jQfNs4V2Usij5KpvDtZAcK546to8GToZCAYC5ZCYq3UDdJZBJEAr8SV2ZBZCGpoOXL61pjyfy9WH1Wbppecl55S7N453EWMDGTSloxAigtoprwuNA94vWX6zkpREKKKbKqdGhgGyGLc2qEZD
        // access_token=CAAUmtWcpcU8BAFR8FHn3ZCHukebXWjoeh9hseL9qm4qhzMs1UEHW7hVNxenjCVHOQuVfZAJICD8TOyCp9xIPNHrrw70ERsRzZCuNbn1uJtvXMZBQs7yq5Gxn6ZCuDEBhBDCrnt5lqD4vCX7bYSTgL0nRdzCC4sfwmHtZB44NgvNdTRHkKfNRyw&expires=5184000
        FacebookClient facebookClient = new DefaultFacebookClient("CAAUmtWcpcU8BAFR8FHn3ZCHukebXWjoeh9hseL9qm4qhzMs1UEHW7hVNxenjCVHOQuVfZAJICD8TOyCp9xIPNHrrw70ERsRzZCuNbn1uJtvXMZBQs7yq5Gxn6ZCuDEBhBDCrnt5lqD4vCX7bYSTgL0nRdzCC4sfwmHtZB44NgvNdTRHkKfNRyw");

        // Fetch Birding Group and start reading posts
        JsonObject group = facebookClient.fetchObject("114204608605113", JsonObject.class);
        Connection<JsonObject> feed = facebookClient.fetchConnection(group.get("id") + "/feed", JsonObject.class);

        // Check whether we have read them already
        PrimaryTreeMap<String, Date> treeMap = recMan.treeMap("PostsRead");

        int i = 0;
        for (List<JsonObject> feedPage : feed) {
            for (JsonObject post : feedPage) {
                // First, make sure that the post has an ID
                if (!post.has("id")) {
                    System.err.println("warning: post has no ID: " + post.toString());
                    continue;
                }

                // Check that we have not read it yet
                String id = post.getString("id");
                if (treeMap.containsKey(id)) {
                    System.err.println("Skipping post " + id + " has it has been read at " + treeMap.get(id));
                    continue;
                }

                // Else, convert and send to fluentd
                i++;
                //System.err.println("Post " + i++);
                String simplifiedPost = outputPosts(post);

                // https://stackoverflow.com/questions/2779251/how-can-i-convert-json-to-a-hashmap-using-gson
                //LinkedTreeMap<String, Object> map = new LinkedTreeMap<>();
                //Gson gson = new Gson();
                System.err.println("Logging post to fluent: " + simplifiedPost);
                //HashMap<String, Object> map = new HashMap<>();
                //map.put("facebookPost", simplifiedPost);
                //LOG.log("post", map);

                JsonObject simplifiedJson = new JsonObject();
                simplifiedJson.put("message", simplifiedPost);
                simplifiedJson.put("time", post.getString("created_time"));
                System.out.println(simplifiedJson.toString());

                // Remember that we posted this
                treeMap.put(id, new Date());
            }
            if (i >= 50) {
                //output.close();
                recMan.commit();
                //LOG.close();
                recMan.close();
                System.exit(0);
            }
        }
    }
}
