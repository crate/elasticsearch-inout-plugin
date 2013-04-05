package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * Internal export response of a shard export request executed directly against a specific shard.
 *
 *
 */
class ShardExportResponse extends BroadcastShardOperationResponse {

    private long count;

    ShardExportResponse() {

    }

    public ShardExportResponse(String index, int shardId, long count) {
        super(index, shardId);
        this.count = count;
    }

    public long getCount() {
        return this.count;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        count = in.readVLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVLong(count);
    }
}
