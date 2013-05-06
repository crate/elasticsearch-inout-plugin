package crate.elasticsearch.action.dump.index;

import java.io.IOException;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class NodeIndexDumpResponse extends NodeOperationResponse implements ToXContent{

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params)
            throws IOException {
        builder.startObject();
        builder.endObject();
        return builder;
    }

    public static NodeIndexDumpResponse readNew(StreamInput in) throws IOException {
        NodeIndexDumpResponse response = new NodeIndexDumpResponse();
        response.readFrom(in);
        return response;
    }

}
