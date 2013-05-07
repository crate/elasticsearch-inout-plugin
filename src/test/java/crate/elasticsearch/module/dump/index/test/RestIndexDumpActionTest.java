package crate.elasticsearch.module.dump.index.test;

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;
import static com.github.tlrx.elasticsearch.test.EsSetup.deleteAll;
import static com.github.tlrx.elasticsearch.test.EsSetup.fromClassPath;
import static com.github.tlrx.elasticsearch.test.EsSetup.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.tlrx.elasticsearch.test.EsSetup;

import crate.elasticsearch.action.dump.index.IndexDumpAction;
import crate.elasticsearch.action.dump.index.IndexDumpRequest;
import crate.elasticsearch.action.dump.index.IndexDumpResponse;
import crate.elasticsearch.action.export.ExportAction;
import crate.elasticsearch.action.export.ExportRequest;
import crate.elasticsearch.action.export.ExportResponse;

public class RestIndexDumpActionTest extends TestCase {

    EsSetup esSetup, esSetup2;

    @Before
    public void setUp() {
        esSetup = new EsSetup();
        esSetup.execute(deleteAll(), createIndex("users").withSettings(
                fromClassPath("essetup/settings/test_a.json")).withMapping("d",
                        fromClassPath("essetup/mappings/test_a.json")).withData(
                                fromClassPath("essetup/data/test_a.json")));
        esSetup.client().admin().indices().prepareRefresh("users").execute();
    }

    @After
    public void tearDown() {
        esSetup.terminate();
        if (esSetup2 != null) {
            esSetup2.terminate();
        }
    }

    /**
     * Without any given pay load the index dump action will export meta data to the
     * default location ``dump/index`` within the data folder of each node
     */
    public void testNoOption() throws IOException {
        deleteDefaultDir();
        IndexDumpResponse response = executeDumpRequest();
        List<Map<String, Object>> infos = getIndexDumps(response);
        assertEquals(1, infos.size());
        assertTrue(infos.get(0).get("mappings_file").toString().matches(".*/nodes/0/dump/index/mappings.json"));
    }

    /**
     * The target directory must exist
     */
    @Test
    public void testBaseDirMustExist() {
        IndexDumpResponse response = executeDumpRequest(
                "{\"directory\": \"/tmp/doesnotexist\"}");
        List<Map<String, Object>> infos = getIndexDumps(response);
        assertEquals(0, infos.size());
        assertEquals(1, response.nodeFailures().size());
        assertTrue(response.nodeFailures().get(0).getDetailedMessage().contains(
                "No such file or directory"));
    }

    /**
     * When the target directory exists, the index sub directory is created.
     * The mappings file contains the mappings of the indexes.
     * @throws IOException
     */
    @Test
    public void testDirectory() throws IOException {
        File dir = new File("/tmp/myDumpTest");
        deleteRecursive(dir);
        dir.mkdir();
        IndexDumpResponse response = executeDumpRequest(
                "{\"directory\": \"/tmp/myDumpTest\"}");
        List<Map<String, Object>> infos = getIndexDumps(response);
        assertEquals(1, infos.size());
        assertNull(response.nodeFailures());
        File f = new File("/tmp/myDumpTest/index/mappings.json");
        assertTrue(f.exists());
        String output = new BufferedReader(new FileReader(f)).readLine();
        assertEquals("{\"users\":{\"d\":{\"properties\":{\"name\":{\"type\":\"string\",\"index\":\"not_analyzed\",\"store\":true,\"omit_norms\":true,\"index_options\":\"docs\"}}}}}", output);
    }

    /**
     * The 'force_overwrite parameter forces existing files to be overwritten.
     */
    @Test
    public void testForceOverwrite() {
        deleteDefaultDir();
        IndexDumpResponse res = executeDumpRequest();
        assertNull(res.nodeFailures());

        res = executeDumpRequest();
        assertEquals(1, res.nodeFailures().size());
        assertTrue(res.nodeFailures().get(0).getDetailedMessage().contains("File exists"));

        res = executeDumpRequest("{\"force_overwrite\": true}");
        assertNull(res.nodeFailures());
        assertEquals(1, getIndexDumps(res).size());
    }

    /**
     * The whole transport action must work with multiple nodes too
     * @throws IOException
     */
    @Test
    public void testWithMultipleNodes() throws IOException {
        deleteDefaultDir();
        // Prepare a second node and wait for relocation
        esSetup2 = new EsSetup();
        esSetup2.execute(index("users", "d").withSource("{\"name\": \"motorbike\"}"));
        esSetup2.client().admin().cluster().prepareHealth().setWaitForGreenStatus().
            setWaitForNodes("2").setWaitForRelocatingShards(0).execute().actionGet();

        IndexDumpResponse response = executeDumpRequest();
        List<Map<String, Object>> infos = getIndexDumps(response);
        assertEquals(2, infos.size());
        assertNull(response.nodeFailures());
        assertTrue(new File(getDefaultDir(0), "index/mappings.json").exists());
        assertTrue(new File(getDefaultDir(1), "index/mappings.json").exists());
    }

    public static Map<String, Object> toMap(ToXContent toXContent) throws IOException {
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        toXContent.toXContent(builder, ToXContent.EMPTY_PARAMS);
        return XContentFactory.xContent(XContentType.JSON).createParser(
                builder.string()).mapOrderedAndClose();
    }

    /**
     * Execute an index dump request with a JSON string as source query. Waits for
     * async callback.
     *
     * @param source
     */
    private IndexDumpResponse executeDumpRequest(String source) {
        IndexDumpRequest indexDumpRequest = createIndexDumpRequest();
        indexDumpRequest.source(source);
        return esSetup.client().execute(IndexDumpAction.INSTANCE, indexDumpRequest).actionGet();
    }

    /**
     * Execute an index dump request without any source query. Waits for
     * async callback.
     */
    private IndexDumpResponse executeDumpRequest() {
        IndexDumpRequest indexDumpRequest = createIndexDumpRequest();
        return esSetup.client().execute(IndexDumpAction.INSTANCE, indexDumpRequest).actionGet();
    }

    /**
     * Add the current mapping meta data to the index dump request, as the rest action does.
     *
     * @return
     */
    private IndexDumpRequest createIndexDumpRequest() {
        IndexDumpRequest indexDumpRequest = new IndexDumpRequest();
        ClusterStateRequest clusterStateRequest = Requests.clusterStateRequest()
                .filterRoutingTable(true)
                .filterNodes(true);

        clusterStateRequest.listenerThreaded(false);

        ClusterStateResponse clusterState = esSetup.client().admin().cluster().state(clusterStateRequest).actionGet();
        MetaData metaData = clusterState.getState().metaData();

        Map<String, List<MappingMetaData>> mappings = new HashMap<String,List<MappingMetaData>>();
        for (IndexMetaData indexMetaData : metaData) {
            List<MappingMetaData> indexMappings = new ArrayList<MappingMetaData>();
            for (MappingMetaData mappingMd : indexMetaData.mappings().values()) {
                    indexMappings.add(mappingMd);
            }
            mappings.put(indexMetaData.index(), indexMappings);
        }
        indexDumpRequest.mappings(mappings);
        return indexDumpRequest;
    }

    private File getDefaultDir(int node) {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.source("{\"output_file\": \"dump\", \"fields\": [\"_source\", \"_id\", \"_index\", \"_type\"], \"force_overwrite\": true, \"explain\": true}");
        ExportResponse explain = esSetup.client().execute(ExportAction.INSTANCE, exportRequest).actionGet();

        try {
            Map<String, Object> res = toMap(explain);
            List<Map<String, String>> list = (ArrayList<Map<String, String>>) res.get("exports");
            return new File(list.get(node).get("output_file"));
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * Helper method to delete an already existing dump directory
     */
    private void deleteDefaultDir() {
        File defaultDir = getDefaultDir(0);
        if (defaultDir.exists()) {
            deleteRecursive(defaultDir);
        }
    }

    private static List<Map<String, Object>> getIndexDumps(IndexDumpResponse resp) {
        Map<String, Object> res = null;
        try {
            res = toMap(resp);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return (List<Map<String, Object>>) res.get("index_dumps");
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
          for (File c : f.listFiles())
            deleteRecursive(c);
        }
        f.delete();
      }
}
