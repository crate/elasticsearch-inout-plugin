package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;

/**
 * Internal export response of a shard export request executed directly against a specific shard.
 *
 *
 */
class ShardExportResponse extends BroadcastShardOperationResponse {

    private String stderr;
    private String stdout;
    private int exitCode;
    private List<String> cmdArray;
    private String cmd;
    private String file;

    ShardExportResponse() {
    }

    public ShardExportResponse(String index, int shardId, String cmd, List<String> cmdArray, String file, String stderr, String stdout, int exitCode) {
        super(index, shardId);
        this.cmd = cmd;
        this.cmdArray = cmdArray;
        this.file = file;
        this.stderr = stderr;
        this.stdout = stdout;
        this.exitCode = exitCode;
    }

    public String getCmd() {
        return cmd;
    }

    public List<String> getCmdArray() {
        return cmdArray;
    }

    public String geFile() {
        return file;
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
        cmd = in.readString();
        file = in.readString();
        stderr = in.readString();
        stdout = in.readString();
        exitCode = in.readInt();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(cmd);
        out.writeString(file);
        out.writeString(stderr);
        out.writeString(stdout);
        out.writeInt(exitCode);
    }
}
