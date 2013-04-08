package com.firstblick.elasticsearch.service.export;

import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.internal.ShardSearchRequest;

import java.util.List;


public class ExportContext extends SearchContext {

    private List<String> outputCmdArray;
    private String outputCmd;
    private String outputFile;

    public ExportContext(long id, ShardSearchRequest request, SearchShardTarget shardTarget, Engine.Searcher engineSearcher, IndexService indexService, IndexShard indexShard, ScriptService scriptService) {
        super(id, request, shardTarget, engineSearcher, indexService, indexShard, scriptService);
    }

    public void outputCmdArray(List<String> outputCmdArray) {
        this.outputCmdArray = outputCmdArray;
    }

    public void outputCmd(String outputCmd) {
        this.outputCmd = outputCmd;
    }

    public void outputFile(String outputFile) {
        this.outputFile = outputFile;
    }
}
