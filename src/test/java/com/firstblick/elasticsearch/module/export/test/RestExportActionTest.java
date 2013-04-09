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

    final CountDownLatch signal = new CountDownLatch(1);
    EsSetup esSetup;

    RestExportAction restExportAction;

    /**
     * Action listener to fail tests onFailure.
     */
    private abstract class SuccessActionListener implements ActionListener<ExportResponse> {

        @Override
        public void onFailure(Throwable e) {
            fail(e.getMessage());
        }

    }

    @Before
    public void setUp() {
        esSetup = new EsSetup();
        esSetup.execute(
                deleteAll(),
               createIndex("users").withSettings(fromClassPath("essetup/settings/test_a.json")).
               withMapping("d", fromClassPath("essetup/mappings/test_a.json")).
               withData(fromClassPath("essetup/data/test_a.json"))
        );
        esSetup.client().admin().indices().prepareRefresh("users").execute();
    }

    @After
    public void tearDown() {
        esSetup.terminate();
    }

    @Test
    public void testPlainCall() {
        ExportRequest exportRequest = new ExportRequest();
        ActionListener<ExportResponse> listener = new SuccessActionListener() {
            @Override
            public void onResponse(ExportResponse response) {
                List<Map<String, Object>> infos = response.getShardInfos();
                assertEquals(2, infos.size());
                assertShardInfo(infos.get(0), "users", 0, "", "", "");
                assertShardInfo(infos.get(1), "users", 0, "", "", "");
                signal.countDown();
            }}
        ;
        esSetup.client().execute(ExportAction.INSTANCE, exportRequest, listener);

        // wait for async callback
        try {
            signal.await(4000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Signal interrupted");
        }
    }

    private void assertShardInfo(Map<String, Object> map, String index,
            int exitcode, String stderr, String stdout, String cmd) {
        assertEquals(index, map.get("index"));
        assertEquals(exitcode, map.get("exitcode"));
        assertEquals(stderr, map.get("stderr"));
        assertEquals(stdout, map.get("stdout"));
        assertEquals(cmd, map.get("cmd"));
    }
}
