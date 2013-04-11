package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

/**
 * The response of the count action.
 */
public class ExportResponse extends BroadcastOperationResponse implements ToXContent {


    List<ShardExportInfo> shardExportInfos;
    private long totalExported;

    ExportResponse(int totalShards, int successfulShards, int failedShards, List<ShardExportInfo> shardExportInfos) {
        super(totalShards, successfulShards, failedShards, null);
        this.shardExportInfos = shardExportInfos;
        for (ShardExportInfo sei : this.shardExportInfos) {
            totalExported += sei.numExported();
        }
    }

    public ExportResponse() {

    }

    public long getTotalExported() {
        return totalExported;
    }


    public List<ShardExportInfo> getShardExportInfos() {
        return shardExportInfos;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startArray("exports");
        for (ShardExportInfo sei : this.shardExportInfos) {
            sei.toXContent(builder, params);
        }
        builder.endArray();
        builder.field("totalExported", totalExported);
        buildBroadcastShardsHeader(builder, this);
        builder.endObject();
        return builder;
    }
}
