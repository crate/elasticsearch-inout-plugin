package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;

import java.util.HashMap;
import java.util.Map;

public class ShardExportInfo {

    String index;
    int shardId;
    ShardOperationFailedException exception;
    ShardExportResponse response;

    protected ShardExportInfo(String index, int shardId) {
        this.index = index;
        this.shardId = shardId;
    }

    public ShardExportInfo(ShardExportResponse shardExportResponse) {
        this(shardExportResponse.getIndex(), shardExportResponse.getShardId());
        this.response = shardExportResponse;
    }

    public ShardExportInfo(BroadcastShardOperationFailedException exception) {
        this(exception.shardId().index().getName(), exception.shardId().getId());
        this.exception = new DefaultShardOperationFailedException(exception);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("index", index);
        ret.put("shard", shardId);
        if (exception != null) {
            ret.put("error", exception);
        } else {
            if (response.geFile() != null) {
                ret.put("output_file", response.geFile());
            } else {
                ret.put("output_cmd", response.getCmd() != null ? response.getCmd() : response.getCmdArray());
                ret.put("stderr", response.getStderr());
                ret.put("stdout", response.getStdout());
                ret.put("exitcode", response.getExitCode());
            }
        }
        return ret;
    }
}
