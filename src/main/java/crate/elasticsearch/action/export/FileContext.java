package crate.elasticsearch.action.export;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.index.engine.Engine.Searcher;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.internal.ShardSearchRequest;

public abstract class FileContext extends SearchContext {

    private static final String VAR_SHARD = "${shard}";
    private static final String VAR_INDEX = "${index}";
    private static final String VAR_CLUSTER = "${cluster}";

    private String outputFile;

    private String nodePath;

    public FileContext(long id, ShardSearchRequest request,
            SearchShardTarget shardTarget, Searcher engineSearcher,
            IndexService indexService, IndexShard indexShard,
            ScriptService scriptService, String nodePath) {
        super(id, request, shardTarget, engineSearcher, indexService, indexShard,
                scriptService);
        this.nodePath = nodePath;
    }

    public String outputFile() {
        return outputFile;
    }

    public void outputFile(String outputFile) {
        outputFile = applyVars(outputFile);
        File outFile = new File(outputFile);
        if (!outFile.isAbsolute() && nodePath != null) {
            outputFile = new File(nodePath, outputFile).getAbsolutePath();
        }
        this.outputFile = outputFile;
    }

    public String nodePath() {
        return nodePath;
    }

    /**
     * Replaces variable placeholder with actual value in all elements of templateArray
     *
     * @param templateArray
     * @return
     */
    protected List<String> applyVars(List<String> templateArray) {
        List<String> ret = new ArrayList<String>();
        for (String part : templateArray) {
            ret.add(applyVars(part));
        }
        return ret;
    }

    /**
     * Replaces variable placeholder with actual value
     *
     * @param template
     * @return
     */
    protected String applyVars(String template) {
        template = template.replace(VAR_SHARD, String.valueOf(indexShard().shardId().getId()));
        template = template.replace(VAR_INDEX, indexShard().shardId().getIndex());
        template = template.replace(VAR_CLUSTER, clusterName());
        return template;
    }

    /**
     * Method to retrieve name of cluster
     *
     * @return name of cluster
     */
    private String clusterName() {
        return ClusterName.clusterNameFromSettings(this.indexShard().indexSettings()).value();
    }
}
