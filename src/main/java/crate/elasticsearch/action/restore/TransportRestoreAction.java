package crate.elasticsearch.action.restore;

import crate.elasticsearch.action.import_.AbstractTransportImportAction;
import crate.elasticsearch.action.restore.parser.RestoreParser;
import crate.elasticsearch.import_.Importer;
import crate.elasticsearch.script.ScriptProvider;

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 *
 */
public class TransportRestoreAction extends AbstractTransportImportAction {

    @Inject
    public TransportRestoreAction(Settings settings, ClusterName clusterName,
                                  ThreadPool threadPool, ClusterService clusterService,
                                  TransportService transportService, ScriptService scriptService, ScriptProvider scriptProvider, RestoreParser restoreParser, Importer importer, NodeEnvironment nodeEnv) {
        super(settings, clusterName, threadPool, clusterService, transportService, scriptService, scriptProvider, restoreParser, importer, nodeEnv);
    }

    @Override
    protected String transportAction() {
        return RestoreAction.NAME;
    }
}
