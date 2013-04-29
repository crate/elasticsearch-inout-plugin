package com.firstblick.elasticsearch.action.import_;

import static org.elasticsearch.common.collect.Lists.newArrayList;

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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import com.firstblick.elasticsearch.action.import_.parser.ImportParser;
import com.firstblick.elasticsearch.import_.Importer;

public class TransportImportAction extends TransportNodesOperationAction<ImportRequest, ImportResponse, NodeImportRequest, NodeImportResponse>{

    private ImportParser importParser;

    private Importer importer;

    @Inject
    public TransportImportAction(Settings settings, ClusterName clusterName,
            ThreadPool threadPool, ClusterService clusterService,
            TransportService transportService, ImportParser importParser, Importer importer) {
        super(settings, clusterName, threadPool, clusterService, transportService);
        this.importParser = importParser;
        this.importer = importer;
    }

    @Override
    protected String transportAction() {
        return ImportAction.NAME;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.INDEX;
    }

    @Override
    protected ImportRequest newRequest() {
        return new ImportRequest();
    }

    @Override
    protected ImportResponse newResponse(ImportRequest request,
            AtomicReferenceArray nodesResponses) {
        int total = nodesResponses.length();
        int successfulNodes = 0;
        int failedNodes = 0;
        List<FailedNodeException> nodeFailures = null;
        List<NodeImportResponse> responses = new ArrayList<NodeImportResponse>();
        for (int i=0; i < total; i++) {
            Object nodeResponse = nodesResponses.get(i);
            if (nodeResponse == null) {
                failedNodes++;
            } else if (nodeResponse instanceof FailedNodeException) {
                failedNodes++;
                if (nodeFailures == null) {
                    nodeFailures = newArrayList();
                }
                nodeFailures.add((FailedNodeException) nodeResponse);
            } else if (nodeResponse instanceof Exception) {
                ((Exception) nodeResponse).getMessage();
            } else {
                responses.add((NodeImportResponse) nodeResponse);
                successfulNodes++;
            }
        }
        return new ImportResponse(responses, total, successfulNodes, failedNodes, nodeFailures);
    }

    /**
     * This method is called on non primary nodes
     */
    @Override
    protected NodeImportRequest newNodeRequest() {
        return new NodeImportRequest();
    }

    /**
     * This method is called on primary node for every node
     */
    @Override
    protected NodeImportRequest newNodeRequest(String nodeId,
            ImportRequest request) {
        return new NodeImportRequest(nodeId, request);
    }

    /**
     * This method is called on primary node for non-primary nodes
     */
    @Override
    protected NodeImportResponse newNodeResponse() {
        return new NodeImportResponse();
    }

    @Override
    protected NodeImportResponse nodeOperation(NodeImportRequest request)
            throws ElasticSearchException {
        ImportContext context = new ImportContext();

        BytesReference source = request.source();
        importParser.parseSource(context, source);
        Importer.Result result = importer.execute(context, request);
        return new NodeImportResponse(clusterService.state().nodes().localNode(), result);
    }

    @Override
    protected boolean accumulateExceptions() {
        return true;
    }
}
