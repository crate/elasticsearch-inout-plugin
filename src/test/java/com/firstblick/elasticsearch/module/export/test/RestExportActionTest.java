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
     * Action listener to fail tests onFailure.
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
                        .withSettings(fromClassPath("essetup/settings/test_a.json"))
                        .withMapping("d",
                                fromClassPath("essetup/mappings/test_a.json"))
                        .withData(fromClassPath("essetup/data/test_a.json")));
        esSetup.client().admin().indices().prepareRefresh("users").execute();
    }

    @After
    public void tearDown() {
        esSetup.terminate();
    }

    @Test
    public void testOutputCommand() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_cmd\": \"cat\", \"fields\": [\"name\"]}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);
        if (response == null) {
            fail("No response within time");
        }

        List<Map<String, Object>> infos = response.getShardInfos();
        assertEquals(2, infos.size());
        // TODO adapt when working
        assertShardInfoCommand(infos.get(0), "users", -1, "", "Command failed",
                null);
        assertShardInfoCommand(infos.get(1), "users", -1, "", "Command failed",
                null);
    }

    @Test
    public void testOutputFile() {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest
                .source("{\"output_file\": \"/tmp/${cluster}.${shard}.${index}.export\", \"fields\": [\"name\"]}");
        ActionListener<ExportResponse> listener = new ExportResponseActionListener();
        esSetup.client()
                .execute(ExportAction.INSTANCE, exportRequest, listener);

        waitForAsyncCallback(4000);
        if (response == null) {
            fail("No response within time");
        }

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

    private void waitForAsyncCallback(long milliSeconds) {
        try {
            signal.await(milliSeconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Signal interrupted");
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
