package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;

import java.util.HashMap;
import java.util.Map;

public class ShardExportInfo {

    String node;
    String index;
    int shardId;
    ShardOperationFailedException exception;
    ShardExportResponse response;

    protected ShardExportInfo(String node, String index, int shardId) {
        this.node = node;
        this.index = index;
        this.shardId = shardId;
    }

    public ShardExportInfo(ShardExportResponse shardExportResponse) {
        this(shardExportResponse.getNode(), shardExportResponse.getIndex(), shardExportResponse.getShardId());
        this.response = shardExportResponse;
    }

    public ShardExportInfo(String node, BroadcastShardOperationFailedException exception) {
        this(node, exception.shardId().index().getName(), exception.shardId().getId());
        this.exception = new DefaultShardOperationFailedException(exception);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("node", node);
        ret.put("index", index);
        ret.put("shard", shardId);
        if (exception != null) {
            ret.put("error", exception);
        } else {
            if (response.getFile() != null) {
                ret.put("output_file", response.getFile());
            } else {
                ret.put("output_cmd", response.getCmd() != null ? response.getCmd() : response.getCmdArray());
                if (!response.dryRun()) {
                    ret.put("stderr", response.getStderr());
                    ret.put("stdout", response.getStdout());
                    ret.put("exitcode", response.getExitCode());
                }
            }
        }
        return ret;
    }
}
