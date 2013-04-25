package com.firstblick.elasticsearch.action.imports;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportImportAction extends TransportNodesOperationAction<ImportRequest, ImportResponse, NodeImportRequest, NodeImportResponse>{

    @Inject
    public TransportImportAction(Settings settings, ClusterName clusterName,
            ThreadPool threadPool, ClusterService clusterService,
            TransportService transportService) {
        super(settings, clusterName, threadPool, clusterService, transportService);
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
        List<NodeImportResponse> responses = new ArrayList<NodeImportResponse>();
        for (int i=0; i < total; i++) {
            Object nodeResponse = nodesResponses.get(i);
            if (nodeResponse == null) {
                failedNodes++;
            } else {
                responses.add((NodeImportResponse) nodeResponse);
                successfulNodes++;
            }
        }
        return new ImportResponse(responses, total, successfulNodes, failedNodes);
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
        return new NodeImportResponse(clusterService.state().nodes().localNode());
    }

    @Override
    protected boolean accumulateExceptions() {
        return false;
    }
}
