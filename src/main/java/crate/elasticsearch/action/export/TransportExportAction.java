package crate.elasticsearch.action.export;

import crate.elasticsearch.action.export.parser.DumpParser;
import crate.elasticsearch.action.export.parser.ExportParser;
import crate.elasticsearch.export.Exporter;
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
public class TransportExportAction extends AbstractTransportExportAction {

    @Inject
    public TransportExportAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                 TransportService transportService, IndicesService indicesService,
                                 ScriptService scriptService, ExportParser exportParser, Exporter exporter) {
        super(settings, threadPool, clusterService, transportService, indicesService, scriptService, exportParser, exporter);
    }

}
