package crate.elasticsearch.action.dump.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import crate.elasticsearch.action.dump.index.parser.IndexDumpParser;

public class TransportIndexDumpAction extends TransportNodesOperationAction<IndexDumpRequest, IndexDumpResponse, NodeIndexDumpRequest, NodeIndexDumpResponse> {

    private final IndexDumpParser indexDumpParser;
    private final IndexDumper indexDumper;
    private String nodePath;

    @Inject
    public TransportIndexDumpAction(Settings settings, ClusterName clusterName,
            ThreadPool threadPool, ClusterService clusterService,
            TransportService transportService, NodeEnvironment nodeEnvironment,
            IndexDumpParser indexDumpParser, IndexDumper indexDumper) {
        super(settings, clusterName, threadPool, clusterService, transportService);
        this.indexDumpParser = indexDumpParser;
        this.indexDumper = indexDumper;
        File[] paths = nodeEnvironment.nodeDataLocations();
        if (paths.length > 0) {
            nodePath = paths[0].getAbsolutePath();
        }
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
        int total = nodesResponses.length();
        List<NodeIndexDumpResponse> responses = new ArrayList<NodeIndexDumpResponse>();
        List<FailedNodeException> nodeFailures = null;
        for (int i = 0; i < total; i++) {
            Object nodeResponse = nodesResponses.get(i);
            if (nodeResponse instanceof FailedNodeException) {
                if (nodeFailures == null) {
                    nodeFailures = new ArrayList<FailedNodeException>();
                }
                nodeFailures.add((FailedNodeException) nodeResponse);
            } else if (nodeResponse instanceof NodeIndexDumpResponse) {
                responses.add((NodeIndexDumpResponse) nodeResponse);
            }
        }
        return new IndexDumpResponse(responses, nodeFailures);
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
        IndexDumpContext context = new IndexDumpContext(nodePath);

        BytesReference source = request.source();
        indexDumpParser.parseSource(context, source);
        IndexDumper.Result result;
        try {
            result = indexDumper.execute(context, request);
        } catch (IOException e) {
            throw new IndexDumpException(e);
        }
        return new NodeIndexDumpResponse(clusterService.state().nodes().localNode(), result);
    }

    @Override
    protected boolean accumulateExceptions() {
        return true;
    }

}
