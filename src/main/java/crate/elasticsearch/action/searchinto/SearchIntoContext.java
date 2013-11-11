package crate.elasticsearch.action.searchinto;

import org.elasticsearch.cache.recycler.CacheRecycler;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.internal.DefaultSearchContext;
import org.elasticsearch.search.internal.ShardSearchRequest;

import crate.elasticsearch.script.IScriptContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container class for inout specific informations.
 */
public class SearchIntoContext extends DefaultSearchContext implements IScriptContext {

    // currently we only support index targets
    private String targetType = "index";

    private List<InetSocketTransportAddress> targetNodes;

    private String scriptString;
    private String scriptLang;
    private Map<String, Object> scriptParams;
    private Map<String, Object> executionContext;
    private ExecutableScript executableScript;
    

    public Map<String, String> outputNames() {
        return outputNames;
    }

    private final Map<String, String> outputNames = new HashMap<String,
            String>();

    public SearchIntoContext(long id, ShardSearchRequest request,
            SearchShardTarget shardTarget, Engine.Searcher engineSearcher,
            IndexService indexService, IndexShard indexShard,
            ScriptService scriptService, CacheRecycler cacheRecycler) {
        super(id, request, shardTarget, engineSearcher, indexService,
                indexShard, scriptService, cacheRecycler);
        this.executionContext = new HashMap<String, Object>();
    }

    public String targetType() {
        // this is currently the only type supported
        return targetType;
    }

    public List<InetSocketTransportAddress> targetNodes() {
        if (targetNodes == null) {
            targetNodes = Lists.newArrayList();
        }
        return targetNodes;
    }

    public void emptyTargetNodes() {
        this.targetNodes = ImmutableList.of();
    }

    @Override
    public String scriptString() {
        return scriptString;
    }

    @Override
    public void scriptString(String scriptString) {
        this.scriptString = scriptString;
    }

    @Override
    public String scriptLang() {
        return scriptLang;
    }

    @Override
    public void scriptLang(String scriptLang) {
        this.scriptLang = scriptLang;
    }

    @Override
    public Map<String, Object> scriptParams() {
        return scriptParams;
    }

    @Override
    public void scriptParams(Map<String, Object> scriptParams) {
        this.scriptParams = scriptParams;
    }

   
    @Override
    public void executableScript(ExecutableScript executableScript) {
        this.executableScript = executableScript;
    }

   
    @Override
    public void executionContext(Map<String, Object> executionContext) {
        this.executionContext = executionContext;
    }

    @Override
	public Map<String, Object> executionContext() {
		return executionContext;
	}

    @Override
	public ExecutableScript executableScript() {
		return executableScript;
	}
    
    

}
