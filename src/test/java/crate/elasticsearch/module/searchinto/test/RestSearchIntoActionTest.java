package crate.elasticsearch.module.searchinto.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.jackson.core.JsonParser;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.json.JsonXContentParser;
import org.junit.Before;
import org.junit.Test;

import crate.elasticsearch.action.import_.ImportResponse;
import crate.elasticsearch.action.searchinto.SearchIntoAction;
import crate.elasticsearch.action.searchinto.SearchIntoRequest;
import crate.elasticsearch.action.searchinto.SearchIntoResponse;
import crate.elasticsearch.module.AbstractRestActionTest;

public class RestSearchIntoActionTest extends AbstractRestActionTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        prepareCreate("test")
                .setSettings(ImmutableSettings.builder().put("index.number_of_shards", 2).build())
                .addMapping("a", "{\"a\":{\"_source\": {\"enabled\": true}}}")
                .execute().actionGet();
        ensureGreen("test");
        index("test", "a", "1", "name", "John");
        refresh();
    }

	@Test
    public void testSearchIntoWithScriptElementModifyingField() {
        SearchIntoRequest request = new SearchIntoRequest("test");
        request.source("{\"fields\": [\"_id\", \"_source\", [\"_index\", \"'newindex'\"]]" + ", \"script\": \"ctx._source.name += ' scripted'\"}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        assertEquals(2, res.getSuccessfulShards());
        List<Map<String, Object>> writes = getWrites(res);
        assertEquals(2, writes.size());
        GetResponse gr = get("newindex", "a", "1", "name");
        assertEquals("John scripted", gr.getField("name").getValue());
     }
	
	@Test
    public void testSearchIntoWithScriptElementAddingField() {
        SearchIntoRequest request = new SearchIntoRequest("test");
        request.source("{\"fields\": [\"_id\", \"_source\", [\"_index\", \"'newindex'\"]]" + ", \"script\": \"ctx._source.name1 =  ctx._source.name + ' scripted'\"}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        assertEquals(2, res.getSuccessfulShards());
        List<Map<String, Object>> writes = getWrites(res);
        assertEquals(2, writes.size());
        refresh();
        GetResponse gr = get("newindex", "a", "1", "name1");
        assertEquals("John scripted", gr.getField("name1").getValue());
     }
	
	@Test
    public void testSearchIntoWithScriptElementDeletingRecord() {
        prepareCreate("newindex")
                .setSettings(ImmutableSettings.builder().put("index.number_of_shards", 1).build())
                .addMapping("a", "{\"a\":{\"_source\": {\"enabled\": true}}}")
                .execute().actionGet();
        ensureGreen("newindex");

        SearchIntoRequest request = new SearchIntoRequest("test");
        request.source("{\"fields\": [\"_id\", \"_source\", [\"_index\", \"'newindex'\"]]" + ", \"script\": \"if (ctx._id == '1') ctx.op = 'delete'; \"}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        assertEquals(2, res.getSuccessfulShards());
        List<Map<String, Object>> writes = getWrites(res);
        assertEquals(2, writes.size());
        GetResponse gr = get("newindex", "a", "1");
        assertFalse(gr.isExists());
     }
	

	@Test
    public void testSearchIntoWithoutSource() {
        prepareCreate("testwithousource")
                .setSettings(ImmutableSettings.builder().put("index.number_of_shards", 1).build())
                .addMapping("a", "{\"a\":{\"_source\": {\"enabled\": false}}}")
                .execute().actionGet();
        ensureGreen("testwithousource");
        index("testwithousource", "a", "1", "name", "John");
        refresh();

        SearchIntoRequest request = new SearchIntoRequest("testwithousource");
        request.source("{\"fields\": [\"_id\", \"_source\", [\"_index\", \"'newindex'\"]]}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        assertEquals(1, res.getFailedShards());
        assertTrue(res.getShardFailures()[0].reason().contains("Parse Failure [The _source field of index testwithousource and type a is not stored.]"));
    }
    

    @Test
    public void testNestedObjectsRewriting() throws IOException  {
        prepareNestedIndex();
        SearchIntoRequest request = new SearchIntoRequest("nested");
        request.source("{\"fields\": [\"_id\", [\"x.city\", \"_source.city\"], [\"x.surname\", \"_source.name.surname\"], [\"x.name\", \"_source.name.name\"], [\"_index\", \"'newindex'\"]]}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        GetResponse getRes = get("newindex", "a", "1");
        assertTrue(getRes.isExists());
        assertEquals("{\"x\":{\"name\":\"Doe\",\"surname\":\"John\",\"city\":\"Dornbirn\"}}", getRes.getSourceAsString());
    }

    @Test
    public void testNestedObjectsRewritingMixed1() throws IOException  {
        prepareNestedIndex();
        SearchIntoRequest request = new SearchIntoRequest("nested");
        request.source("{\"fields\": [\"_id\", [\"x\", \"_source.city\"], [\"x.surname\", \"_source.name.surname\"], [\"x.name\", \"_source.name.name\"], [\"_index\", \"'newindex'\"]]}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        assertTrue(res.getShardFailures()[0].reason().contains("Error on rewriting objects: Mixed objects and values]"));
    }

    @Test
    public void testNestedObjectsRewritingMixed2() throws IOException  {
        prepareNestedIndex();
        SearchIntoRequest request = new SearchIntoRequest("nested");
        request.source("{\"fields\": [\"_id\", [\"x.surname.bad\", \"_source.city\"], [\"x.surname\", \"_source.name.surname\"], [\"x.name\", \"_source.name.name\"], [\"_index\", \"'newindex'\"]]}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        assertTrue(res.getShardFailures()[0].reason().contains("Error on rewriting objects: Mixed objects and values]"));
    }

    @Test
    public void testNestedObjectsRewritingMixed3() throws IOException {
        prepareNestedIndex();
        SearchIntoRequest request = new SearchIntoRequest("nested");
        request.source("{\"fields\": [\"_id\", [\"x.surname\", \"_source.city\"], [\"x.surname.bad\", \"_source.name.surname\"], [\"x.name\", \"_source.name.name\"], [\"_index\", \"'newindex'\"]]}");
        SearchIntoResponse res = cluster().masterClient().execute(SearchIntoAction.INSTANCE, request).actionGet();
        assertTrue(res.getShardFailures()[0].reason().contains("Error on rewriting objects: Mixed objects and values]"));
    }


    private void prepareNestedIndex() throws IOException {
        prepareCreate("nested")
                .setSettings(ImmutableSettings.builder().put("index.number_of_shards", 2).build())
                .addMapping("a", "{\"a\": {\"properties\": {\"name\": {\"properties\": {\"surname\":{\"type\":\"string\"}, \"name\": {\"type\":\"string\"}}}, \"city\": {\"type\": \"string\"}}}}")
                .execute().actionGet();
        ensureGreen("nested");

        index("nested", "a", "1", XContentFactory.jsonBuilder()
                .startObject()
                .field("city", "Dornbirn")
                .startObject("name")
                .field("surname", "John")
                .field("name", "Doe")
                .endObject()
                .endObject());
        refresh();
    }

    private static List<Map<String, Object>> get(SearchIntoResponse resp, String key) {
        Map<String, Object> res = null;
        try {
            res = toMap(resp);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return (List<Map<String, Object>>) res.get(key);
    }
    
    private static List<Map<String, Object>> getWrites(SearchIntoResponse resp) {
        return get(resp, "writes");
    }
    


}
