package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;


/**
 * Internal export request executed directly against a specific index shard.
 */
class ShardExportRequest extends BroadcastShardOperationRequest {

    private float minScore;

    private BytesReference querySource;

    private String[] types = Strings.EMPTY_ARRAY;

    @Nullable
    private String[] filteringAliases;

    ShardExportRequest() {

    }

    public ShardExportRequest(String index, int shardId, @Nullable String[] filteringAliases, ExportRequest request) {
        super(index, shardId, request);
        this.minScore = request.minScore();
        this.querySource = request.querySource();
        this.types = request.types();
        this.filteringAliases = filteringAliases;
    }

    public float minScore() {
        return minScore;
    }

    public BytesReference querySource() {
        return querySource;
    }

    public String[] types() {
        return this.types;
    }

    public String[] filteringAliases() {
        return filteringAliases;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        minScore = in.readFloat();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        System.out.printf("### Exporting: Cluster:", "XX", "Index:", index(), "Shard:", shardId());
    }
}
