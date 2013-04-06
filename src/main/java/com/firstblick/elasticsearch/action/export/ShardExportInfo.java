package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;

import java.util.HashMap;
import java.util.Map;

public class ShardExportInfo {

    String index;
    int shardId;
    String cmd;
    String stderr;
    String stdout;
    int exitcode;
    ShardOperationFailedException exception;

    protected ShardExportInfo(String index, int shardId) {
        this.index = index;
        this.shardId = shardId;
    }

    public ShardExportInfo(ShardExportResponse shardExportResponse) {
        this(shardExportResponse.getIndex(), shardExportResponse.getShardId());
        this.cmd = shardExportResponse.getCmd();
        this.stderr = shardExportResponse.getStderr();
        this.stdout = shardExportResponse.getStdout();
        this.exitcode = shardExportResponse.getExitCode();
    }

    public ShardExportInfo(BroadcastShardOperationFailedException exception) {
        this(exception.shardId().index().getName(), exception.shardId().getId());
        this.exception = new DefaultShardOperationFailedException(exception);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("index", index);
        ret.put("shardId", shardId);
        if (exception != null) {
            ret.put("error", exception);
        } else {
            ret.put("cmd", cmd);
            ret.put("stderr", stderr);
            ret.put("stdout", stdout);
            ret.put("exitcode", exitcode);
        }
        return ret;
    }
}
