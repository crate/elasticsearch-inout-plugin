package crate.elasticsearch.action.dump.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.nodes.NodesOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;


public class IndexDumpResponse extends NodesOperationResponse<NodeIndexDumpResponse> implements ToXContent {

    private List<NodeIndexDumpResponse> responses;
    private List<FailedNodeException> nodeFailures;

    public IndexDumpResponse() {
    }

    public IndexDumpResponse(List<NodeIndexDumpResponse> responses, List<FailedNodeException> nodeFailures) {
        this.responses = responses;
        this.nodeFailures = nodeFailures;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params)
            throws IOException {
        builder.startObject();
        builder.startArray("index_dumps");
        for (NodeIndexDumpResponse r : this.responses) {
            r.toXContent(builder, params);
        }
        builder.endArray();
        if (nodeFailures != null && nodeFailures.size() > 0) {
            builder.startArray("failures");
            for (FailedNodeException failure : nodeFailures) {
                builder.startObject();
                builder.field("node_id", failure.nodeId());
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
        int responsesCount = in.readInt();
        this.responses = new ArrayList<NodeIndexDumpResponse>(responsesCount);
        for (int i = 0; i < responsesCount; i++) {
            responses.add(NodeIndexDumpResponse.readNew(in));
        }
        int failuresCount = in.readInt();
        this.nodeFailures = new ArrayList<FailedNodeException>(failuresCount);
        for (int i = 0; i < failuresCount; i++) {
            String nodeId = in.readString();
            String msg = in.readOptionalString();
            FailedNodeException e = new FailedNodeException(nodeId, msg, null);
            nodeFailures.add(e);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(responses.size());
        for (NodeIndexDumpResponse response : responses) {
            response.writeTo(out);
        }
        out.writeInt(nodeFailures.size());
        for (FailedNodeException e : nodeFailures) {
            out.writeString(e.nodeId());
            out.writeOptionalString(e.getMessage());
        }
    }

    public List<FailedNodeException> nodeFailures() {
        return nodeFailures;
    }
}
