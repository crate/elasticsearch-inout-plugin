package crate.elasticsearch.export;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.version.VersionFetchSubPhase;

import crate.elasticsearch.action.export.ExportContext;

public class Exporter {

    private static final ESLogger logger = Loggers.getLogger(Exporter.class);

    public static class Result {
        public Output.Result outputResult;
        public long numExported;
    }

    private final FetchSubPhase[] fetchSubPhases;

    @Inject
    public Exporter(VersionFetchSubPhase versionPhase) {
        this.fetchSubPhases = new FetchSubPhase[]{versionPhase};
    }

    public Result execute(ExportContext context) {
        logger.info("exporting {}/{}", context.shardTarget().index(),
                context.shardTarget().getShardId());
        Query query = context.query();

        Output output = context.createOutput();
        context.version(true);
        try {
            output.open();
        } catch (IOException e) {
            throw new ExportException(context, "Failed to open output: ", e);
        }
        ExportCollector collector = new ExportCollector(context, output.getOutputStream(), fetchSubPhases);
        try {
            context.searcher().search(query, collector);
        } catch (IOException e) {
            throw new ExportException(context, "Failed to fetch docs", e);
        }
        try {
            output.close();
        } catch (IOException e) {
            throw new ExportException(context, "Failed to close output: ", e);
        }
        Result res = new Result();
        res.outputResult = output.result();
        res.numExported = collector.numExported();
        logger.info("exported {} docs from {}/{}",
                collector.numExported(),
                context.shardTarget().index(),
                context.shardTarget().getShardId());

        return res;
    }

}
