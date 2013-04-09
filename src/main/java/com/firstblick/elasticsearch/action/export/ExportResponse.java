package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The response of the count action.
 */
public class ExportResponse extends BroadcastOperationResponse {

    List<ShardExportInfo> shardExportInfos;

    ExportResponse() {

    }

    ExportResponse(int totalShards, int successfulShards, int failedShards, List<ShardExportInfo> shardExportInfos) {
        super(totalShards, successfulShards, failedShards, null);
        this.shardExportInfos = shardExportInfos;
    }

    /**
     * Method to retrieve export specific informations per shard
     *
     * @return list of shard infos
     */
    public List<Map<String, Object>> getShardInfos() {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        for (ShardExportInfo sei : this.shardExportInfos) {
            ret.add(sei.asMap());
        }
        return ret;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
