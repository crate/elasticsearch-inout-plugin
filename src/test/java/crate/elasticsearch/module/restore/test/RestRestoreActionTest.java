package crate.elasticsearch.module.restore.test;

import crate.elasticsearch.action.dump.DumpAction;
import crate.elasticsearch.action.export.ExportAction;
import crate.elasticsearch.action.export.ExportRequest;
import crate.elasticsearch.action.export.ExportResponse;
import crate.elasticsearch.action.import_.ImportRequest;
import crate.elasticsearch.action.import_.ImportResponse;
import crate.elasticsearch.action.restore.RestoreAction;
import crate.elasticsearch.module.AbstractRestActionTest;

import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;

public class RestRestoreActionTest extends AbstractRestActionTest {

    /**
     * Restore previously dumped data from the default location
     */
    @Test
    public void testRestoreDumpedData() throws IOException {

        deleteDefaultDir();

        setupTestIndexLikeUsers("test", false);
        index("test", "d", "1", "name", "item1");
        index("test", "d", "2", "name", "item2");
        refresh();

        // dump data and recreate empty index
        executeDumpRequest("");

        // delete all
        wipeIndices("test");
        waitForRelocation();

        // run restore without pyload relative directory
        ImportResponse response = executeRestoreRequest("");
        List<Map<String, Object>> imports = getImports(response);
        assertEquals(2, imports.size());

        assertTrue(existsWithField("1", "name", "item1", "test", "d"));
        assertTrue(existsWithField("2", "name", "item2", "test", "d"));

        ClusterStateRequest clusterStateRequest = Requests.clusterStateRequest().metaData(true).indices("test");
        IndexMetaData metaData = cluster().masterClient().admin().cluster().state(clusterStateRequest).actionGet().getState().metaData().index("test");
        assertEquals("{\"d\":{\"properties\":{\"name\":{\"type\":\"string\",\"index\":\"not_analyzed\",\"store\":true,\"norms\":{\"enabled\":false},\"index_options\":\"docs\"}}}}",
                metaData.mappings().get("d").source().toString());
        assertEquals(2, metaData.numberOfShards());
        assertEquals(0, metaData.numberOfReplicas());
    }


    private boolean existsWithField(String id, String field, String value, String index, String type) {
        GetResponse res = get(index, type, id); // rb.setType(type).setId(id).execute().actionGet();
        return res.isExists() && res.getSourceAsMap().get(field).equals(value);
    }

    private static List<Map<String, Object>> getImports(ImportResponse resp) {
        return get(resp, "imports");
    }

    private static List<Map<String, Object>> get(ImportResponse resp, String key) {
        Map<String, Object> res = null;
        try {
            res = toMap(resp);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return (List<Map<String, Object>>) res.get(key);
    }

    private ImportResponse executeRestoreRequest(String source) {
        ImportRequest request = new ImportRequest();
        request.source(source);
        return cluster().masterClient().execute(RestoreAction.INSTANCE, request).actionGet();
    }

    private ExportResponse executeDumpRequest(String source) {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.source(source);
        return cluster().masterClient().execute(DumpAction.INSTANCE, exportRequest).actionGet();
    }

    /**
     * Helper method to delete an already existing dump directory
     */
    private void deleteDefaultDir() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.source("{\"output_file\": \"dump\", \"fields\": [\"_source\", \"_id\", \"_index\", \"_type\"], \"force_overwrite\": true, \"explain\": true}");
        ExportResponse explain = cluster().masterClient().execute(ExportAction.INSTANCE, exportRequest).actionGet();

        try {
            Map<String, Object> res = toMap(explain);
            List<Map<String, String>> list = (ArrayList<Map<String, String>>) res.get("exports");
            for (Map<String, String> map : list) {
                File defaultDir = new File(map.get("output_file").toString());
                if (defaultDir.exists()) {
                    for (File c : defaultDir.listFiles()) {
                        c.delete();
                    }
                    defaultDir.delete();
                }
            }
        } catch (IOException e) {
        }
    }

}
