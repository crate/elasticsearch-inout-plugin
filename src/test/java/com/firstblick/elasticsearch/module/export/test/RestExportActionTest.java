package com.firstblick.elasticsearch.module.export.test;

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;
import static com.github.tlrx.elasticsearch.test.EsSetup.deleteAll;
import static com.github.tlrx.elasticsearch.test.EsSetup.fromClassPath;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.elasticsearch.action.ActionListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.firstblick.elasticsearch.action.export.ExportAction;
import com.firstblick.elasticsearch.action.export.ExportRequest;
import com.firstblick.elasticsearch.action.export.ExportResponse;
import com.firstblick.elasticsearch.rest.action.admin.export.RestExportAction;
import com.github.tlrx.elasticsearch.test.EsSetup;

public class RestExportActionTest extends TestCase {

    ExportResponse response;
    CountDownLatch signal;
    EsSetup esSetup;
    RestExportAction restExportAction;

    /**
     * Action listener for export responses. Save response to member variable
     * and release the count down signal.
     */
    private class ExportResponseActionListener implements
            ActionListener<ExportResponse> {

        @Override
        public void onResponse(ExportResponse res) {
            response = res;
            signal.countDown();
        }

        @Override
        public void onFailure(Throwable e) {
            fail(e.getMessage());
        }

    }

    @Before
    public void setUp() {
        signal = new CountDownLatch(1);
        response = null;
        esSetup = new EsSetup();
        esSetup.execute(
                deleteAll(),
                createIndex("users")
                        .withSettings(
                                fromClassPath("essetup/settings/test_a.json"))
                        .withMapping("d",
                                fromClassPath("essetup/mappings/test_a.json"))
                        .withData(fromClassPath("essetup/data/test_a.json")));
        esSetup.client().admin().indices().prepareRefresh("users").execute();
    }

    @After
    public void tearDown() {
        esSetup.terminate();
    }

    /**
     * Either one of the parameters 'output_cmd' or 'output_file' is required.
     */
    @Test
    public void testNoCommandOrFile() {
        ExportRequest exportRequest = new ExportRequest();
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        assertTrue(infos.get(0).get("error").toString()
                .contains("'output_cmd' or 'output_file' has not been defined"));
        assertTrue(infos.get(1).get("error").toString()
                .contains("'output_cmd' or 'output_file' has not been defined"));
    }

    /**
     * The parameter 'fields' is required.
     */
    @Test
    public void testNoExportFields() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.source("{\"output_cmd\": \"cat\"}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        assertTrue(infos.get(0).get("error").toString()
                .contains("No export fields defined"));
        assertTrue(infos.get(1).get("error").toString()
                .contains("No export fields defined"));
    }

    /**
     * Invalid parameters lead to an error response.
     */
    @Test
    public void testBadParserArgument() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_cmd\": \"cat\", \"fields\": [\"name\"], \"badparam\":\"somevalue\"}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        assertTrue(infos.get(0).get("error").toString()
                .contains("No parser for element [badparam]"));
        assertTrue(infos.get(1).get("error").toString()
                .contains("No parser for element [badparam]"));
    }

    /**
     * The 'output_cmd' parameter can be a single command and is executed.
     * The response shows the index, the executed command, the exit code of
     * the process and the process' standard out and standard error logs
     * (first 8K).
     */
    @Test
    public void testSingleOutputCommand() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_cmd\": \"cat\", \"fields\": [\"name\"]}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        // TODO adapt when working
        assertShardInfoCommand(infos.get(0), "users", -1, "", "Command failed",
                null);
        assertShardInfoCommand(infos.get(1), "users", -1, "", "Command failed",
                null);
    }

    /**
     * The 'output_cmd' parameter can also be a list of arguments.
     */
    @Test
    public void testOutputCommandList() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_cmd\": [\"/bin/sh\", \"-c\", \"cat\"], \"fields\": [\"name\"]}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        // TODO adapt when working
        assertShardInfoCommand(infos.get(0), "users", -1, "", "Command failed",
                null);
        assertShardInfoCommand(infos.get(1), "users", -1, "", "Command failed",
                null);
    }

    /**
     * The 'output_file' parameter defines the filename to save the export.
     * There are 3 template variables that will be replaced:
     *
     *  - ${cluster} : will be replaced with the cluster name
     *  - ${index}   : will be replaced with the index name
     *  - ${shard}   : will be replaced with the shard name
     *
     * The response contains the index, the shard number and the generated
     * output file name.
     */
    @Test
    public void testOutputFile() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_file\": \"/tmp/${cluster}.${shard}.${index}.export\", \"fields\": [\"name\"]}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        assertEquals("users", infos.get(0).get("index"));
        assertEquals("users", infos.get(1).get("index"));
        String output_file = infos.get(0).get("output_file").toString();
        assertTrue(output_file.startsWith("/tmp/"));
        assertTrue(output_file.endsWith(".0.users.export"));
        output_file = infos.get(1).get("output_file").toString();
        assertTrue(output_file.startsWith("/tmp/"));
        assertTrue(output_file.endsWith(".1.users.export"));
    }

    /**
     * Only one parameter of the two 'output_file' or 'output_cmd' can be used.
     */
    @Test
    public void testOutputFileAndOutputCommand() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_file\": \"/filename\", \"output_cmd\": \"cat\", \"fields\": [\"name\"]}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        assertTrue(infos.get(0).get("error").toString()
                .contains("Concurrent definition of 'output_cmd' and 'output_file'"));
        assertTrue(infos.get(1).get("error").toString()
                .contains("Concurrent definition of 'output_cmd' and 'output_file'"));

    }

    /**
     * The 'force_override' parameter forces existing files to be overwritten.
     */
    @Test
    public void testForceOverride() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_file\": \"/tmp/filename.export\", \"fields\": [\"name\"], \"force_override\": \"true\"}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        assertEquals("/tmp/filename.export", infos.get(0).get("output_file").toString());
        assertEquals("/tmp/filename.export", infos.get(1).get("output_file").toString());
        // TODO evaluate if force override works when implemented
    }

    /**
     * The explain parameter does a dry-run without running the command. The
     * response therefore does not contain the stderr, stdout and exitcode values.
     */
    @Test
    public void testExplain() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_cmd\": \"cat\", \"fields\": [\"name\"], \"explain\": \"true\"}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        Map<String, Object> shard_info = infos.get(0);
        assertFalse(shard_info.containsKey("stderr"));
        assertFalse(shard_info.containsKey("stdout"));
        assertFalse(shard_info.containsKey("exitcode"));
    }

    /**
     * Wait for the signal and let the test fail if the response did not
     * retrieve within time.
     * @param milliSeconds time to wait for maximum
     */
    private void waitForAsyncCallback(long milliSeconds) {
        try {
            signal.await(milliSeconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Signal interrupted");
        }
        if (response == null) {
            fail("No response within time");
        }
    }

    private void assertShardInfoCommand(Map<String, Object> map, String index,
            int exitcode, String stdout, String stderr, String cmd) {
        assertEquals(index, map.get("index"));
        assertEquals(exitcode, map.get("exitcode"));
        assertEquals(stderr, map.get("stderr"));
        assertEquals(stdout, map.get("stdout"));
        assertEquals(cmd, map.get("cmd"));
    }
}
