package crate.elasticsearch.module.import_.test;

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;
import static com.github.tlrx.elasticsearch.test.EsSetup.deleteAll;
import static com.github.tlrx.elasticsearch.test.EsSetup.fromClassPath;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.tlrx.elasticsearch.test.EsSetup;

import crate.elasticsearch.action.export.ExportAction;
import crate.elasticsearch.action.export.ExportRequest;
import crate.elasticsearch.action.import_.ImportAction;
import crate.elasticsearch.action.import_.ImportRequest;
import crate.elasticsearch.action.import_.ImportResponse;

public class RestImportActionTest extends TestCase {

    EsSetup node1, node2;

    @Before
    public void setUp() {
        // create two nodes and wait for synchronization
        node1 = new EsSetup();
        node1.execute(deleteAll(), createIndex("users").withSettings(
                fromClassPath("essetup/settings/test_a.json")).withMapping("d",
                        fromClassPath("essetup/mappings/test_a.json")));
    }

    @After
    public void tearDown() {
        node1.terminate();
        if (node2 != null) {
            node2.terminate();
        }
    }

    /**
     * An import directory must be specified in the post data of the request, otherwise
     * an 'No directory defined' exception is delivered in the output.
     */
    @Test
    public void testNoDirectory() {
        setUpSecondNode();
        ImportResponse response = executeImportRequest("{}");
        assertEquals(0, getImports(response).size());
        List<Map<String, Object>> failures = getImportFailures(response);
        assertEquals(2, failures.size());
        assertTrue(failures.get(0).toString().contains("No directory defined"));
    }

    /**
     * A normal import on a single node delivers the node ids of the executing nodes,
     * the time in milliseconds for each node, and the imported files of each nodes
     * with numbers of successful and failing import objects.
     */
    @Test
    public void testImportWithIndexAndType() {
        String path = getClass().getResource("import_1").getPath();
        ImportResponse response = executeImportRequest("{\"directory\": \"" + path + "\"}");
        List<Map<String, Object>> imports = getImports(response);
        assertEquals(1, imports.size());
        Map<String, Object> nodeInfo = imports.get(0);
        assertNotNull(nodeInfo.get("node_id"));
        assertTrue(Long.valueOf(nodeInfo.get("took").toString()) > 0);
        assertEquals("[{file_name=import_1.json, successes=2, failures=0}]",
                nodeInfo.get("imported_files").toString());
        assertTrue(existsWithField("102", "name", "102"));
        assertTrue(existsWithField("103", "name", "103"));
    }

    /**
     * If the type or the index are not given whether in the request URI nor
     * in the import line, the corresponding objects are not imported.
     */
    @Test
    public void testImportWithoutIndexOrType() {
        String path = getClass().getResource("import_2").getPath();
        ImportResponse response = executeImportRequest("{\"directory\": \"" + path + "\"}");
        List<Map<String, Object>> imports = getImports(response);
        Map<String, Object> nodeInfo = imports.get(0);
        assertEquals("[{file_name=import_2.json, successes=1, failures=3}]",
                nodeInfo.get("imported_files").toString());
        assertTrue(existsWithField("202", "name", "202"));
        assertFalse(existsWithField("203", "name", "203"));
        assertFalse(existsWithField("204", "name", "204"));
        assertFalse(existsWithField("205", "name", "205"));
    }

    /**
     * If the index and/or type are given in the URI, all objects are imported
     * into the given index/type.
     */
    @Test
    public void testImportIntoIndexAndType() {
        String path = getClass().getResource("import_2").getPath();
        ImportRequest request = new ImportRequest();
        request.index("another_index");
        request.type("e");
        request.source("{\"directory\": \"" + path + "\"}");
        ImportResponse response = node1.client().execute(ImportAction.INSTANCE, request).actionGet();

        List<Map<String, Object>> imports = getImports(response);
        Map<String, Object> nodeInfo = imports.get(0);
        assertEquals("[{file_name=import_2.json, successes=4, failures=0}]",
                nodeInfo.get("imported_files").toString());
        assertTrue(existsWithField("202", "name", "202", "another_index", "e"));
        assertTrue(existsWithField("203", "name", "203", "another_index", "e"));
        assertTrue(existsWithField("204", "name", "204", "another_index", "e"));
        assertTrue(existsWithField("205", "name", "205", "another_index", "e"));
    }

    /**
     * On bad import files, only the readable lines will be imported, the rest is
     * put to the failure count. (e.g. empty lines, or bad JSON structure)
     */
    @Test
    public void testCorruptFile() {
        String path = getClass().getResource("import_3").getPath();
        ImportResponse response = executeImportRequest("{\"directory\": \"" + path + "\"}");
        List<Map<String, Object>> imports = getImports(response);
        assertEquals(1, imports.size());
        assertEquals("[{file_name=import_3.json, successes=3, failures=2}]",
                imports.get(0).get("imported_files").toString());
    }

    /**
     * The fields _routing, _ttl and _timestamp can be imported. The ttl value
     * is always from now to the end date, no matter if a time stamp value is set.
     * Invalidated objects will not be imported (when actual time is above ttl time stamp).
     */
    @Test
    public void testFields() {
        node1.execute(deleteAll(), createIndex("test").withSettings(
                fromClassPath("essetup/settings/test_a.json")).withMapping("d",
                        "{\"d\": {\"_timestamp\": {\"enabled\": true, \"store\": \"yes\"}}}"));

        long now = new Date().getTime();
        long ttl = 1867329687097L - now;
        String path = getClass().getResource("import_4").getPath();
        ImportResponse response = executeImportRequest("{\"directory\": \"" + path + "\"}");
        List<Map<String, Object>> imports = getImports(response);
        assertEquals(1, imports.size());
        Map<String, Object> nodeInfo = imports.get(0);
        assertNotNull(nodeInfo.get("node_id"));
        assertTrue(Long.valueOf(nodeInfo.get("took").toString()) > 0);
        assertEquals("[{file_name=import_4.json, successes=2, failures=0, invalidated=1}]",
                nodeInfo.get("imported_files").toString());

        GetRequestBuilder rb = new GetRequestBuilder(node1.client(), "test");
        GetResponse res = rb.setType("d").setId("402").setFields("_ttl", "_timestamp", "_routing").execute().actionGet();
        assertEquals("the_routing", res.getField("_routing").getValue());
        assertTrue(ttl - Long.valueOf(res.getField("_ttl").getValue().toString()) < 10000);
        assertEquals(1367329785380L, res.getField("_timestamp").getValue());

        res = rb.setType("d").setId("403").setFields("_ttl", "_timestamp").execute().actionGet();
        assertTrue(ttl - Long.valueOf(res.getField("_ttl").getValue().toString()) < 10000);
        assertTrue(now - Long.valueOf(res.getField("_timestamp").getValue().toString()) < 10000);

        assertFalse(existsWithField("404", "name", "404"));
    }

    /**
     * With multiple nodes every node is handled and delivers correct JSON. Every
     * found file in the given directory on the node's system is handled.
     * Note that this test runs two nodes on the same file system, so the same
     * files are imported twice.
     */
    @Test
    public void testMultipleFilesAndMultipleNodes() {
        setUpSecondNode();
        String path = getClass().getResource("import_5").getPath();
        ImportResponse response = executeImportRequest("{\"directory\": \"" + path + "\"}");
        List<Map<String, Object>> imports = getImports(response);
        assertEquals(2, imports.size());

        String result = "[{file_name=import_5_a.json, successes=1, failures=0}, {file_name=import_5_b.json, successes=1, failures=0}]";
        Map<String, Object> nodeInfo = imports.get(0);
        assertNotNull(nodeInfo.get("node_id"));
        assertTrue(Long.valueOf(nodeInfo.get("took").toString()) > 0);
        assertEquals(result, nodeInfo.get("imported_files").toString());
        nodeInfo = imports.get(1);
        assertNotNull(nodeInfo.get("node_id"));
        assertTrue(Long.valueOf(nodeInfo.get("took").toString()) > 0);
        assertEquals(result, nodeInfo.get("imported_files").toString());

        assertTrue(existsWithField("501", "name", "501"));
        assertTrue(existsWithField("511", "name", "511"));
    }

    /**
     * Set up a second node and wait  for green status
     */
    private void setUpSecondNode() {
        node2 = new EsSetup();
        node2.execute(deleteAll());
        node2.client().admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
    }

    private boolean existsWithField(String id, String field, String value) {
        return existsWithField(id, field, value, "test", "d");
    }

    private boolean existsWithField(String id, String field, String value, String index, String type) {
        GetRequestBuilder rb = new GetRequestBuilder(node1.client(), index);
        GetResponse res = rb.setType(type).setId(id).execute().actionGet();
        return res.isExists() && res.getSourceAsMap().get(field).equals(value);
    }

    private static Map<String, Object> toMap(ToXContent toXContent) throws IOException {
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        toXContent.toXContent(builder, ToXContent.EMPTY_PARAMS);
        return XContentFactory.xContent(XContentType.JSON).createParser(
                builder.string()).mapOrderedAndClose();
    }

    private static List<Map<String, Object>> getImports(ImportResponse resp) {
        return get(resp, "imports");
    }

    private static List<Map<String, Object>> getImportFailures(ImportResponse resp) {
        return get(resp, "failures");
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

    private ImportResponse executeImportRequest(String source) {
        ImportRequest request = new ImportRequest();
        request.source(source);
        return node1.client().execute(ImportAction.INSTANCE, request).actionGet();
    }

}
