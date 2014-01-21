package crate.elasticsearch.action.searchinto;

import crate.elasticsearch.action.searchinto.parser.SearchIntoParser;
import crate.elasticsearch.script.ScriptProvider;
import crate.elasticsearch.searchinto.Writer;
import org.elasticsearch.cache.recycler.CacheRecycler;
import org.elasticsearch.cache.recycler.PageCacheRecycler;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;


/**
 *
 */
public class TransportSearchIntoAction extends AbstractTransportSearchIntoAction {

    @Inject
    public TransportSearchIntoAction(Settings settings,
                                     ThreadPool threadPool, ClusterService clusterService,
                                     TransportService transportService,
                                     CacheRecycler cacheRecycler, PageCacheRecycler pageRecycler,
                                     IndicesService indicesService, ScriptService scriptService,
                                     ScriptProvider scriptProvider,
                                     SearchIntoParser parser, Writer writer) {
        super(settings, threadPool, clusterService, transportService,
                cacheRecycler, pageRecycler, indicesService,
                scriptService, scriptProvider, parser, writer);
    }

    @Override
    protected String transportAction() {
        return SearchIntoAction.NAME;
    }

}
