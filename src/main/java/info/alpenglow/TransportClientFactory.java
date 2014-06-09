package info.alpenglow;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

class TransportClientFactory {

    private static void checkIndexExists(Client client, String indexName) {
        try {
            client.get(new GetRequest(indexName)).actionGet();
        } catch (ElasticsearchException e) {
            System.err.println("Index " + indexName + " not found");
            client.admin().indices().create(new CreateIndexRequest(indexName));
        }
    }

    public static TransportClient createClient(String indexName) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("properties/config.properties"));

        // Connect to ElasticSearch
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", prop.getProperty("cluster.name")).build();

        TransportClient client = new TransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(prop.getProperty("server.name"), 9300));

        checkIndexExists(client, indexName);

        return client;
    }
}
