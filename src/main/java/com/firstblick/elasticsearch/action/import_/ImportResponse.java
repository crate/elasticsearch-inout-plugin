package com.firstblick.elasticsearch.action.import_;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.nodes.NodesOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class ImportResponse extends NodesOperationResponse<NodeImportResponse> implements ToXContent {

    private List<NodeImportResponse> responses;
    private List<FailedNodeException> nodeFailures;

    public ImportResponse() {
    }

    public ImportResponse(List<NodeImportResponse> responses, int total,
            int successfulNodes, int failedNodes, List<FailedNodeException> nodeFailures) {
        this.responses = responses;
        this.nodeFailures = nodeFailures;
    }

    public List<NodeImportResponse> getResponses() {
        return responses;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params)
            throws IOException {
        builder.startObject();
        builder.startArray("imports");
        for (NodeImportResponse r : this.responses) {
            r.toXContent(builder, params);
        }
        builder.endArray();
        if (nodeFailures != null && nodeFailures.size() > 0) {
            builder.startArray("failures");
            for (FailedNodeException failure : nodeFailures) {
                builder.startObject();
                builder.field("nodeId", failure.nodeId());
                builder.field("reason", failure.getDetailedMessage());
                builder.endObject();
            }
            builder.endArray();
        }
        builder.endObject();
        return builder;
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