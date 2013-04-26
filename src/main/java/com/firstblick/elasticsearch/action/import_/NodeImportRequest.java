package com.firstblick.elasticsearch.action.import_;

import java.io.IOException;

import org.elasticsearch.action.support.nodes.NodeOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class NodeImportRequest  extends NodeOperationRequest {

    public String nodeId;

    NodeImportRequest() {
    }

    public NodeImportRequest(String nodeId, ImportRequest request) {
        super(request, nodeId);
        this.nodeId = nodeId;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
