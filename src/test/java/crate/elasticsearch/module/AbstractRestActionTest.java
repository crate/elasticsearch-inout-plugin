package crate.elasticsearch.module;

import crate.elasticsearch.plugin.inout.InOutPlugin;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Before;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Abstract base class for the plugin's rest action tests. Sets up the client
 * and delivers some base functionality needed for all tests.
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE, numNodes = 2)
public abstract class AbstractRestActionTest extends ElasticsearchIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings settings = ImmutableSettings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("plugin.types", InOutPlugin.class.getName())
                .put("index.number_of_shards", defaultShardCount())
                .put("index.number_of_replicas", 0)
                .put("http.enabled", false)
                .build();
        return settings;
    }

    @Override
    public Settings indexSettings() {
        return settingsBuilder().put("index.number_of_shards", defaultShardCount()).put("index.number_of_replicas", 0).build();
    }

    public void setupTestIndexLikeUsers(String indexName, int shards, boolean loadTestData) throws IOException {
        prepareCreate(indexName).setSettings(settingsBuilder().put("index.number_of_shards", shards).put("index.number_of_replicas", 0))
                .addMapping("d", jsonBuilder().startObject()
                        .startObject("d")
                        .startObject("properties")
                        .startObject("name")
                        .field("type", "string")
                        .field("index", "not_analyzed")
                        .field("store", "yes")
                        .endObject()
                        .endObject()
                        .endObject())
                .execute().actionGet();
        ensureGreen(indexName);

        if (loadTestData) {
            index(indexName, "d", "1", "name", "car");
            index(indexName, "d", "2", "name", "bike");
            index(indexName, "d", "3", "name", "train");
            index(indexName, "d", "4", "name", "bus");
        }
        refresh();
        waitForRelocation();

    }

    public void setupTestIndexLikeUsers(String indexName, boolean loadTestData) throws IOException {
        setupTestIndexLikeUsers(indexName, defaultShardCount(), loadTestData);
    }

    protected int defaultShardCount() {
        return 2;
    }

    protected int defaultNodeCount() {
        return 2;
    }


    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        cluster().ensureAtLeastNumNodes(defaultNodeCount());
        cluster().ensureAtMostNumNodes(defaultNodeCount());
        setupTestIndexLikeUsers("users", defaultShardCount(), true);
    }

    protected final GetResponse get(String index, String type, String id, String... fields) {
        return client().prepareGet(index, type, id).setFields(fields).execute().actionGet();
    }

    /**
     * Convert an XContent object to a Java map
     *
     * @param toXContent
     * @return
     * @throws IOException
     */
    public static Map<String, Object> toMap(ToXContent toXContent) throws IOException {
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        toXContent.toXContent(builder, ToXContent.EMPTY_PARAMS);
        return XContentFactory.xContent(XContentType.JSON).createParser(
                builder.string()).mapOrderedAndClose();
    }

    /**
     * Get a list of lines from a gzipped file.
     * Test fails if file not found or IO exception happens.
     *
     * @param filename the file name to read
     * @return a list of strings
     */
    protected List<String> readLinesFromGZIP(String filename) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(new File(filename)))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IO Excsption while reading ZIP stream");
        }
        return readLines(filename, reader);
    }

    protected List<String> readLines(String filename, BufferedReader reader) {
        List<String> lines = new ArrayList<String>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("IO Exception occured while reading file");
        }
        return lines;
    }


}
