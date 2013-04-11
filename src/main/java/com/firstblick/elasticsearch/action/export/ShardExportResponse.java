package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.text.Text;

import java.io.IOException;
import java.util.List;

/**
 * Internal export response of a shard export request executed directly against a specific shard.
 */
class ShardExportResponse extends BroadcastShardOperationResponse {

    private String stderr;
    private String stdout;
    private int exitCode;
    private List<String> cmdArray;
    private String cmd;
    private String file;
    private boolean dryRun = false;
    private Text node;
    private long numExported;

    ShardExportResponse() {
    }

    /**
     * Constructor for regular cases
     *
     * @param node Name of the Node
     * @param index Name of the index
     * @param shardId ID of the shard
     * @param cmd executed command (might be null)
     * @param cmdArray executed command array (might be null)
     * @param file written file (might be null)
     * @param stderr output written to standard error by the executed command
     * @param stdout output written to standard out by the executed command
     * @param exitCode exit code of the executed command
     * @param numExported number of exported documents
     *
     */
    public ShardExportResponse(Text node, String index, int shardId,
                               String cmd, List<String> cmdArray, String file,
                               String stderr, String stdout, int exitCode,
                               long numExported) {
        super(index, shardId);
        this.node = node;
        this.cmd = cmd;
        this.cmdArray = cmdArray;
        this.file = file;
        this.stderr = stderr;
        this.stdout = stdout;
        this.exitCode = exitCode;
        this.numExported = numExported;
    }

    /**
     * Constructor for dry runs. Does not contain any execution infos
     *
     * @param node Name of the Node
     * @param index Name of the index
     * @param shardId ID of the shard
     * @param cmd executed command (might be null)
     * @param cmdArray executed command array (might be null)
     * @param file written file (might be null)
     */
    public ShardExportResponse(Text node, String index, int shardId, String cmd, List<String> cmdArray, String file) {
        super(index, shardId);
        this.node = node;
        this.cmd = cmd;
        this.cmdArray = cmdArray;
        this.file = file;
        this.dryRun = true;
    }

    public String getCmd() {
        return cmd;
    }

    public List<String> getCmdArray() {
        return cmdArray;
    }

    public String getFile() {
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

    public long getNumExported() {
        return numExported;
    }


    public boolean dryRun() {
        return dryRun;
    }

    public Text getNode() {
        return node;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        cmd = in.readString();
        file = in.readString();
        stderr = in.readString();
        stdout = in.readString();
        exitCode = in.readInt();
        numExported = in.readLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(cmd);
        out.writeString(file);
        out.writeString(stderr);
        out.writeString(stdout);
        out.writeInt(exitCode);
        out.writeLong(numExported);
    }
}
