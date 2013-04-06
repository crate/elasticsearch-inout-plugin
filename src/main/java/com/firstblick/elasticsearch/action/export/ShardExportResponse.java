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

    private String stderr;
    private String stdout;
    private int exitCode;

    ShardExportResponse() {

    }

    public ShardExportResponse(String index, int shardId, String stderr, String stdout, int exitCode) {
        super(index, shardId);
        this.stderr = stderr;
        this.stdout = stdout;
        this.exitCode = exitCode;
    }

    public String getStderr() {
        return stderr;
    }

    public String getStdout() {
        return stdout;
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        stderr = in.readString();
        stdout = in.readString();
        exitCode = in.readInt();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(stderr);
        out.writeString(stdout);
        out.writeInt(exitCode);
    }
}
