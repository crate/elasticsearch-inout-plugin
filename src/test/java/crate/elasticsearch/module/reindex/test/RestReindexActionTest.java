package crate.elasticsearch.module.reindex.test;

import crate.elasticsearch.action.reindex.ReindexAction;
import crate.elasticsearch.action.searchinto.SearchIntoRequest;
import crate.elasticsearch.action.searchinto.SearchIntoResponse;
import crate.elasticsearch.module.AbstractRestActionTest;
import crate.elasticsearch.plugin.inout.InOutPlugin;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RestReindexActionTest extends AbstractRestActionTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings settings = ImmutableSettings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("gateway.type", "local")
                .build();
        return settings;
    }

    @Test
    public void testSearchIntoWithoutSource() {
        prepareCreate("nosource")
                .setSettings(ImmutableSettings.builder().put("index.number_of_shards", 1).build())
                .addMapping("a", "{\"a\":{\"_source\": {\"enabled\": false}}}")
                .execute().actionGet();
        ensureGreen("nosource");
        index("nosource", "a", "1", "name", "John");
        refresh();

        SearchIntoRequest request = new SearchIntoRequest("nosource");
        SearchIntoResponse res = cluster().masterClient().execute(ReindexAction.INSTANCE, request).actionGet();
        assertEquals(1, res.getFailedShards());
        assertTrue("error message is wrong: " + res.getShardFailures()[0].reason(), res.getShardFailures()[0].reason().contains("Parse Failure [The _source field of index nosource and type a is not stored.]"));

    }

    @Test
    public void testReindexToSelf() {
        prepareCreate("test")
                .setSettings("{\"index\": {\"number_of_shards\":1,\n" +
                        "\"number_of_replicas\":0,\n" +
                        "\"analysis\": {\"analyzer\": {" +
                        "\"stopper\": {\"type\": \"stop\", \"stopwords\": [\"guy\"]}" +
                        "}}}}")
                .addMapping("a", "{\"_source\": {\"enabled\": true, \"store\": true}, \"properties\": {\"name\": {\"type\": \"string\", \"index_analyzer\": \"stopper\", \"search_analyzer\": \"simple\", \"store\": \"yes\"}}}")
                .execute().actionGet();
        ensureGreen("test");
        index("test", "a", "1", "name", "a nice guy man");
        refresh();

        SearchResponse respFound = cluster().masterClient().prepareSearch("test").setQuery(QueryBuilders.matchQuery("name", "nice")).execute().actionGet();
        assertEquals(1, respFound.getHits().getTotalHits());

        SearchResponse respAbsent = cluster().masterClient().prepareSearch("test").setQuery(QueryBuilders.matchQuery("name", "guy")).execute().actionGet();
        assertEquals(0, respAbsent.getHits().getTotalHits());

        CountResponse respCountBefore = cluster().masterClient().prepareCount("test").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
        assertEquals(1, respCountBefore.getCount());

        CloseIndexResponse respClose = cluster().masterClient().admin().indices().prepareClose("test").execute().actionGet();
        assertTrue(respClose.isAcknowledged());

        UpdateSettingsResponse respUpd = cluster().masterClient().admin().indices().prepareUpdateSettings("test")
                .setSettings("{\"analysis\": {\"analyzer\": {" +
                "\"stopper\": {\"type\": \"stop\", \"stopwords\": [\"nice\"]}" +
                "}}}").execute().actionGet();
        assertTrue(respUpd.isAcknowledged());

        OpenIndexResponse respOpen = cluster().masterClient().admin().indices().prepareOpen("test").execute().actionGet();
        assertTrue(respOpen.isAcknowledged());

        ensureGreen("test");

        CountResponse respCountAfterOpen = cluster().masterClient().prepareCount("test").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
        assertEquals(1, respCountAfterOpen.getCount());

        SearchIntoRequest request = new SearchIntoRequest("test");
        SearchIntoResponse res = cluster().masterClient().execute(ReindexAction.INSTANCE, request).actionGet();
        String errMsg = "FAILURE!";
        if (res.getFailedShards() > 0) {
            for (int i = 0; i < res.getShardFailures().length; i++) {
                errMsg += "\n  " + res.getShardFailures()[i].reason();
            }
        }
        assertEquals(errMsg, 0, res.getFailedShards());

        CountResponse respCountAfterReindex = cluster().masterClient().prepareCount("test").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
        assertEquals(1, respCountAfterReindex.getCount());

        SearchResponse respNowAbsent = cluster().masterClient().prepareSearch("test").setQuery(QueryBuilders.matchQuery("name", "nice")).execute().actionGet();
        assertEquals(0, respNowAbsent.getHits().getTotalHits());

        SearchResponse respNowFound = cluster().masterClient().prepareSearch("test").setQuery(QueryBuilders.matchQuery("name", "guy")).execute().actionGet();
        assertEquals(1, respNowFound.getHits().getTotalHits());


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
}
