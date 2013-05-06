package crate.elasticsearch.action.dump.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.support.nodes.NodeOperationRequest;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class NodeIndexDumpRequest extends NodeOperationRequest {

    private Map<String, List<MappingMetaData>> mappings;

    public NodeIndexDumpRequest() {
    }

    public NodeIndexDumpRequest(String nodeId, IndexDumpRequest request) {
        super(request, nodeId);
        this.mappings = request.mappings();
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int mappingsCount = in.readInt();
        mappings = new HashMap<String, List<MappingMetaData>>();
        for (int i = 0; i < mappingsCount; i++) {
            String index = in.readString();
            int typeCount = in.readInt();
            List<MappingMetaData> list = new ArrayList<MappingMetaData>();
            for (int j = 0; j < typeCount; j++) {
                list.add(MappingMetaData.readFrom(in));
            }
            mappings.put(index, list);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(mappings.size());
        for (String mappingkey : mappings.keySet()) {
            out.writeString(mappingkey);
            List<MappingMetaData> list = mappings.get(mappingkey);
            out.writeInt(list.size());
            for (MappingMetaData metaData : list) {
                MappingMetaData.writeTo(metaData, out);
            }
        }
    }

    public Map<String, List<MappingMetaData>> mappings() {
        return mappings;
    }
}
