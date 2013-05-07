package crate.elasticsearch.action.dump.index;

import java.io.IOException;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import crate.elasticsearch.action.dump.index.IndexDumper.Result;

public class NodeIndexDumpResponse extends NodeOperationResponse implements ToXContent{

    private IndexDumper.Result result;

    NodeIndexDumpResponse() {
    }

    public NodeIndexDumpResponse(DiscoveryNode localNode, Result result) {
        super(localNode);
        this.result = result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params)
            throws IOException {
        builder.startObject();
        builder.field("node_id", this.getNode().id());
        builder.field("mappings_file", result.mappingsFile);
        builder.endObject();
        return builder;
    }

    public static NodeIndexDumpResponse readNew(StreamInput in) throws IOException {
        NodeIndexDumpResponse response = new NodeIndexDumpResponse();
        response.readFrom(in);
        return response;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        result = new IndexDumper.Result();
        result.mappingsFile = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(result.mappingsFile);
    }
}
