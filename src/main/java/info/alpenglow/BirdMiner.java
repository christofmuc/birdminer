package info.alpenglow;


import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.json.JsonObject;

public class BirdMiner {

    public static void main(final String args[]) {
        System.out.println("Starting BirdMiner, connecting to Facebook!");

        FacebookClient facebookClient = new DefaultFacebookClient("CAACEdEose0cBAONAHVAVHCUi27hH21P5b2zeFLJr7inFFPNGzGPZC8ehvIlfZC5IDDk2MrXZBxoHVoMZBjfDEgrZCBotqFSDAIfcODExZAocZBTmhHKfAZBBkFIlaLOtpgKTa3No3DDiDGH7c6ytDLz2IiOO0TLDe8MWzlE2MiL1plr6p39w8EBQI72hXMCPjbIZD");

        JsonObject group = facebookClient.fetchObject("114204608605113", JsonObject.class);
        JsonObject feed = facebookClient.fetchObject(group.get("id") + "/feed", JsonObject.class);
        System.out.println(feed.toString());
    }
}
