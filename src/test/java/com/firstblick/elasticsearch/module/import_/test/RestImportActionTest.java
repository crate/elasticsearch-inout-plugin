package com.firstblick.elasticsearch.module.import_.test;

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;
import static com.github.tlrx.elasticsearch.test.EsSetup.deleteAll;
import static com.github.tlrx.elasticsearch.test.EsSetup.fromClassPath;

import java.io.IOException;
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

import com.firstblick.elasticsearch.action.import_.ImportAction;
import com.firstblick.elasticsearch.action.import_.ImportRequest;
import com.firstblick.elasticsearch.action.import_.ImportResponse;
import com.github.tlrx.elasticsearch.test.EsSetup;

public class RestImportActionTest extends TestCase {

    EsSetup node1, node2;

    @Before
    public void setUp() {
        // create two nodes and wait for synchronization
        node1 = new EsSetup();
        node2 = new EsSetup();
        node2.execute(deleteAll());
        node1.execute(deleteAll(), createIndex("users").withSettings(
                fromClassPath("essetup/settings/test_a.json")).withMapping("d",
                        fromClassPath("essetup/mappings/test_a.json")));
        node2.client().admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
    }

    @After
    public void tearDown() {
        node1.terminate();
        node2.terminate();
    }

    /**
     * An import directory must be specified in the post data of the request, otherwise
     * an 'No directory defined' exception is delivered in the output.
     */
    @Test
    public void testNoDirectory() {
        ImportResponse response = executeImportRequest("{}");
        assertEquals(0, getImports(response).size());
        List<Map<String, Object>> failures = getImportFailures(response);
        assertEquals(2, failures.size());
        assertTrue(failures.get(0).toString().contains("No directory defined"));
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
