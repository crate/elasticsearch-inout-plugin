package com.firstblick.elasticsearch.module.export.test;

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
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.firstblick.elasticsearch.action.export.ExportAction;
import com.firstblick.elasticsearch.action.export.ExportRequest;
import com.firstblick.elasticsearch.action.export.ExportResponse;
import com.firstblick.elasticsearch.rest.action.admin.export.RestExportAction;
import com.github.tlrx.elasticsearch.test.EsSetup;

public class RestExportActionTest extends TestCase {

    EsSetup esSetup;
    RestExportAction restExportAction;

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
    }

    public static Map<String, Object> toMap(ToXContent toXContent) throws IOException {
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        toXContent.toXContent(builder, ToXContent.EMPTY_PARAMS);
        return XContentFactory.xContent(XContentType.JSON).createParser(
                builder.string()).mapOrderedAndClose();
    }

    /**
     * Either one of the parameters 'output_cmd' or 'output_file' is required.
     */
    @Test
    public void testNoCommandOrFile() throws IOException {
        ExportResponse response = executeExportRequest("{\"fields\": [\"name\"]}");
        assertEquals(2, response.getShardFailures().length);
        assertTrue(response.getShardFailures()[0].reason().contains(
                "'output_cmd' or 'output_file' has not been defined"));
        assertTrue(response.getShardFailures()[1].reason().contains(
                "'output_cmd' or 'output_file' has not been defined"));
    }

    /**
     * The parameter 'fields' is required.
     */
    @Test
    public void testNoExportFields() {
        ExportResponse response = executeExportRequest("{\"output_cmd\": \"cat\"}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(0, infos.size());
        assertEquals(2, response.getShardFailures().length);
        assertTrue(response.getShardFailures()[0].reason().contains(
                "No export fields defined"));
        assertTrue(response.getShardFailures()[1].reason().contains(
                "No export fields defined"));
    }

    /**
     * Invalid parameters lead to an error response.
     */
    @Test
    public void testBadParserArgument() {
        ExportResponse response = executeExportRequest(
                "{\"output_cmd\": \"cat\", \"fields\": [\"name\"], \"badparam\":\"somevalue\"}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(0, infos.size());
        assertEquals(2, response.getShardFailures().length);
        assertTrue(response.getShardFailures()[0].reason().contains(
                "No parser for element [badparam]"));
        assertTrue(response.getShardFailures()[1].reason().contains(
                "No parser for element [badparam]"));
    }

    /**
     * The 'output_cmd' parameter can be a single command and is executed. The
     * response shows the index, the node name, the shard number, the executed
     * command, the exit code of the process and the process' standard out and
     * standard error logs (first 8K) of every shard result.
     */
    @Test
    public void testSingleOutputCommand() {
        ExportResponse response = executeExportRequest(
                "{\"output_cmd\": \"cat\", \"fields\": [\"name\"]}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(2, infos.size());
        assertShardInfoCommand(infos.get(0), "users", 0,
                "{\"name\":\"car\"}\n{\"name\":\"train\"}\n", "", null);
        assertShardInfoCommand(infos.get(1), "users", 0,
                "{\"name\":\"bike\"}\n{\"name\":\"bus\"}\n", "", null);
    }

    /**
     * The 'output_cmd' parameter can also be a list of arguments.
     */
    @Test
    public void testOutputCommandList() {
        ExportResponse response = executeExportRequest(
                "{\"output_cmd\": [\"/bin/sh\", \"-c\", \"cat\"], \"fields\": [\"name\"]}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(2, infos.size());
        assertShardInfoCommand(infos.get(0), "users", 0,
                "{\"name\":\"car\"}\n{\"name\":\"train\"}\n", "", null);
        assertShardInfoCommand(infos.get(1), "users", 0,
                "{\"name\":\"bike\"}\n{\"name\":\"bus\"}\n", "", null);
    }

    /**
     * The 'output_file' parameter defines the filename to save the export.
     * There are 3 template variables that will be replaced:
     * <p/>
     * - ${cluster} : will be replaced with the cluster name - ${index} : will
     * be replaced with the index name - ${shard} : will be replaced with the
     * shard name
     * <p/>
     * The response contains the index, the shard number, the node name and the
     * generated output file name of every shard result.
     */
    @Test
    public void testOutputFile() {
        String clusterName = esSetup.client().admin().cluster().prepareHealth().
                setWaitForGreenStatus().execute().actionGet().getClusterName();
        String filename_0 = "/tmp/" + clusterName + ".0.users.export";
        String filename_1 = "/tmp/" + clusterName + ".1.users.export";
        new File(filename_0).delete();
        new File(filename_1).delete();

        ExportResponse response = executeExportRequest(
                "{\"output_file\": \"/tmp/${cluster}.${shard}.${index}.export\", \"fields\": [\"name\", \"_id\"]}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(2, infos.size());
        Map<String, Object> shard_0 = infos.get(0);
        Map<String, Object> shard_1 = infos.get(1);
        assertEquals("users", shard_0.get("index"));
        assertEquals("users", shard_1.get("index"));
        String output_file_0 = shard_0.get("output_file").toString();
        assertEquals(filename_0, output_file_0);
        String output_file_1 = shard_1.get("output_file").toString();
        assertEquals(filename_1, output_file_1);
        assertTrue(shard_0.containsKey("node"));
        assertTrue(shard_1.containsKey("node"));

        List<String> lines_0 = readLines(filename_0);
        assertEquals(2, lines_0.size());
        assertEquals("{\"name\":\"car\",\"_id\":\"1\"}", lines_0.get(0));
        assertEquals("{\"name\":\"train\",\"_id\":\"3\"}", lines_0.get(1));
        List<String> lines_1 = readLines(filename_1);
        assertEquals(2, lines_1.size());
        assertEquals("{\"name\":\"bike\",\"_id\":\"2\"}", lines_1.get(0));
        assertEquals("{\"name\":\"bus\",\"_id\":\"4\"}", lines_1.get(1));
    }

    /**
     * Only one parameter of the two 'output_file' or 'output_cmd' can be used.
     */
    @Test
    public void testOutputFileAndOutputCommand() {
        ExportResponse response = executeExportRequest(
                "{\"output_file\": \"/filename\", \"output_cmd\": \"cat\", \"fields\": [\"name\"]}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(0, infos.size());
        assertEquals(0, infos.size());
        assertEquals(2, response.getShardFailures().length);
        assertTrue(response.getShardFailures()[0].reason().contains(
                "Concurrent definition of 'output_cmd' and 'output_file'"));
        assertTrue(response.getShardFailures()[1].reason().contains(
                "Concurrent definition of 'output_cmd' and 'output_file'"));

    }

    /**
     * The 'force_overwrite' parameter forces existing files to be overwritten.
     */
    @Test
    public void testForceOverwrite() {
        String filename = "/tmp/filename.export";
        ExportResponse response = executeExportRequest("{\"output_file\": \"" + filename +
                "\", \"fields\": [\"name\"], \"force_overwrite\": \"true\"}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(2, infos.size());
        assertEquals("/tmp/filename.export", infos.get(0).get("output_file").toString());
        assertEquals("/tmp/filename.export", infos.get(1).get("output_file").toString());
        List<String> lines = readLines(filename);
        assertEquals(2, lines.size());
        assertEquals("{\"name\":\"bike\"}", lines.get(0));
    }

    /**
     * The explain parameter does a dry-run without running the command. The
     * response therefore does not contain the stderr, stdout and exitcode
     * values.
     */
    @Test
    public void testExplainCommand() {
        ExportResponse response = executeExportRequest(
                "{\"output_cmd\": \"cat\", \"fields\": [\"name\"], \"explain\": \"true\"}");

        List<Map<String, Object>> infos = getExports(response);
        assertEquals(2, infos.size());
        Map<String, Object> shard_info = infos.get(0);
        assertFalse(shard_info.containsKey("stderr"));
        assertFalse(shard_info.containsKey("stdout"));
        assertFalse(shard_info.containsKey("exitcode"));
        assertSame(shard_info.keySet(), infos.get(0).keySet());
    }

    /**
     * The explain parameter does a dry-run without writing to the file.
     */
    @Test
    public void testExplainFile() {
        String filename = "/tmp/explain.txt";
        new File(filename).delete();

        executeExportRequest("{\"output_file\": \"" + filename +
                "\", \"fields\": [\"name\"], \"explain\": \"true\"}");

        assertFalse(new File(filename).exists());
    }

    /**
     * Export request must also work with multiple nodes.
     */
    @Test
    public void testWithMultipleNodes() {
        // Prepare a second node and wait for relocation
        EsSetup esSetup2 = new EsSetup();
        esSetup2.execute(index("users", "d").withSource("{\"name\": \"motorbike\"}"));
        esSetup2.client().admin().cluster().prepareHealth().setWaitForGreenStatus().
            setWaitForNodes("2").setWaitForRelocatingShards(0).execute().actionGet();

        // Do export request
        String source = "{\"output_cmd\": \"cat\", \"fields\": [\"name\"]}";
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.source(source);
        ExportResponse response = esSetup2.client().execute(
                ExportAction.INSTANCE, exportRequest).actionGet();

        // The two shard results are from different nodes and have no failures
        assertEquals(0, response.getFailedShards());
        List<Map<String, Object>> infos = getExports(response);
        assertNotSame(infos.get(0).get("node"), infos.get(1).get("node"));
    }

    /**
     * A query must also work and deliver only the queried results.
     */
    @Test
    public void testWithQuery() {
        ExportResponse response = executeExportRequest(
                "{\"output_file\": \"/tmp/query-${shard}.json\", \"fields\": [\"name\"], " +
                "\"query\": {\"match\": {\"name\":\"bus\"}}, \"force_overwrite\": true}");

        assertEquals(0, response.getFailedShards());
        List<Map<String, Object>> infos = getExports(response);
        assertEquals(2, infos.size());

        List<String> lines_0 = readLines("/tmp/query-0.json");
        assertEquals(0, lines_0.size());
        List<String> lines_1 = readLines("/tmp/query-1.json");
        assertEquals(1, lines_1.size());
        assertEquals("{\"name\":\"bus\"}", lines_1.get(0));
    }

    private static List<Map<String, Object>> getExports(ExportResponse resp) {
        Map<String, Object> res = null;
        try {
            res = toMap(resp);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return (List<Map<String, Object>>) res.get("exports");
    }

    /**
     * Execute an export request with a JSON string as source query. Waits for
     * async callback and writes result in response member variable.
     *
     * @param source
     */
    private ExportResponse executeExportRequest(String source) {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.source(source);
        return esSetup.client().execute(ExportAction.INSTANCE, exportRequest).actionGet();
    }

    /**
     * Get a list of lines from a file.
     * Test fails if file not found or IO exception happens.
     *
     * @param filename the file name to read
     * @return a list of strings
     */
    private List<String> readLines(String filename) {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(filename)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found");
        }
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

    private void assertShardInfoCommand(Map<String, Object> map, String index,
            int exitcode, String stdout, String stderr, String cmd) {
        assertEquals(index, map.get("index"));
        assertEquals(exitcode, map.get("exitcode"));
        assertEquals(stderr, map.get("stderr"));
        assertEquals(stdout, map.get("stdout"));
        assertEquals(cmd, map.get("cmd"));
        assertTrue(map.containsKey("node"));
    }
}
