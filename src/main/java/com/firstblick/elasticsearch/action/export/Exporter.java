package com.firstblick.elasticsearch.action.export;

import com.firstblick.elasticsearch.export.ExportException;
import com.firstblick.elasticsearch.export.Output;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.logging.ESLogger;

import java.io.IOException;

public class Exporter {

    public static Output.Result execute(ESLogger logger, ExportContext context) {
        logger.info("exporter execute");
        Query query = context.query();

        Output output = context.createOutput();

        try {
            output.open();
        } catch (IOException e) {
            throw new ExportException(context,
                    "Failed to open output: ", e);
        }
        ExportCollector collector = new ExportCollector(context, logger,
                output.getOutputStream());
        try {
            context.searcher().search(query, collector);
        } catch (IOException e) {
            throw new ExportException(context,
                    "Failed to fetch docs", e);
        }
        try {
            output.close();
        } catch (IOException e) {
            throw new ExportException(context,
                    "Failed to close output: ", e);
        }
        return output.result();
    }

}
