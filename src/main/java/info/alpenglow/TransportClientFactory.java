package info.alpenglow;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class TransportClientFactory {


    public static TransportClient createClient() throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("properties/config.properties"));

        // Connect to ElasticSearch
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", prop.getProperty("cluster.name")).build();

        return new TransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(prop.getProperty("server.name"), 9300));
    }
}
