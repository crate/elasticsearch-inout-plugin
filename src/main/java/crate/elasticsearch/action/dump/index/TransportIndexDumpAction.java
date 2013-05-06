package crate.elasticsearch.action.dump.index;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportIndexDumpAction extends TransportNodesOperationAction<IndexDumpRequest, IndexDumpResponse, NodeIndexDumpRequest, NodeIndexDumpResponse> {

    @Inject
    public TransportIndexDumpAction(Settings settings, ClusterName clusterName,
            ThreadPool threadPool, ClusterService clusterService,
            TransportService transportService) {
        super(settings, clusterName, threadPool, clusterService, transportService);
    }

    @Override
    protected String transportAction() {
        return IndexDumpAction.NAME;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.INDEX;
    }

    @Override
    protected IndexDumpRequest newRequest() {
        return new IndexDumpRequest();
    }

    @Override
    protected IndexDumpResponse newResponse(IndexDumpRequest request,
            AtomicReferenceArray nodesResponses) {
        return new IndexDumpResponse();
    }

    @Override
    protected NodeIndexDumpRequest newNodeRequest() {
        return new NodeIndexDumpRequest();
    }

    @Override
    protected NodeIndexDumpRequest newNodeRequest(String nodeId,
            IndexDumpRequest request) {
        return new NodeIndexDumpRequest(nodeId, request);
    }

    @Override
    protected NodeIndexDumpResponse newNodeResponse() {
        return new NodeIndexDumpResponse();
    }

    @Override
    protected NodeIndexDumpResponse nodeOperation(NodeIndexDumpRequest request)
            throws ElasticSearchException {
        Map<String, List<MappingMetaData>> map = request.mappings();
        for (String key : map.keySet()) {
            System.out.println(key + " : " + map.get(key));
        }
        return new NodeIndexDumpResponse();
    }

    @Override
    protected boolean accumulateExceptions() {
        return true;
    }

}
