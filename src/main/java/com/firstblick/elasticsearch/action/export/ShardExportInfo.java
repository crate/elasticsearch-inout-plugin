package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * Container class holding information about export execution. Might contain proper response informations
 * or a Exception.
 */
public class ShardExportInfo implements ToXContent {

    Text node;
    String index;
    int shardId;
    ShardOperationFailedException exception;
    ShardExportResponse response;
    private long numExported;

    protected ShardExportInfo(String index, int shardId) {
        this.index = index;
        this.shardId = shardId;
    }

    /**
     * Constructor for success case
     *
     * @param shardExportResponse
     */
    public ShardExportInfo(ShardExportResponse shardExportResponse) {
        this(shardExportResponse.getIndex(), shardExportResponse.getShardId());
        this.node = shardExportResponse.getNode();
        this.response = shardExportResponse;
        this.numExported = response.getNumExported();
    }

    /**
     * Constructor for failure case
     *
     * @param exception
     */
    public ShardExportInfo(BroadcastShardOperationFailedException exception) {
        this(exception.shardId().index().getName(), exception.shardId().getId());
        this.exception = new DefaultShardOperationFailedException(exception);
    }

    public long numExported() {
        return numExported;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("index", index);
        builder.field("shard", shardId);
        if (exception != null) {
            builder.field("error", exception);
        } else {
            builder.field("node", node);
            builder.field("numExported", response.getNumExported());
            if (response.getFile() != null) {
                builder.field("output_file", response.getFile());
            } else {
                builder.field("output_cmd", response.getCmd() != null ? response.getCmd() : response.getCmdArray());
                if (!response.dryRun()) {
                    builder.field("stderr", response.getStderr());
                    builder.field("stdout", response.getStdout());
                    builder.field("exitcode", response.getExitCode());
                }
            }
        }
        builder.endObject();
        return builder;
    }
}
