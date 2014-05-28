package info.alpenglow;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author mlappsch
 *         Copyright 2014 by Avid Technology, Inc.
 */
public class TransportClientFactory {


    public static TransportClient createClient() throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("config.properties"));

        // Connect to ElasticSearch
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", prop.getProperty("cluster.name")).build();
        TransportClient client = new TransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(prop.getProperty("server.name"), 9300));

        return client;
    }
}
