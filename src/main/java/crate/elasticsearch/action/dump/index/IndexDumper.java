package crate.elasticsearch.action.dump.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;

public class IndexDumper {

    public Result execute(IndexDumpContext context, NodeIndexDumpRequest request) throws IOException {
        boolean overwrite = context.forceOverride();
        File dumpDir = new File(context.directory());
        Result result = new Result();
        result.mappingsFile = createMappingsFile(overwrite, dumpDir, request.mappings());
        return result;
    }

    private String createMappingsFile(boolean overwrite, File dumpDir,
            Map<String, List<MappingMetaData>> map) throws IOException {
        File indexDir = new File(dumpDir, "index");
        if (!indexDir.exists()) {
            indexDir.mkdir();
        }
        File mappingsFile = new File(indexDir, "mappings.json");
        if (mappingsFile.exists() && !overwrite) {
            throw new IOException("File exists: " + mappingsFile);
        }
        OutputStream os = new FileOutputStream(mappingsFile);
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        builder.startObject();
        for (String index : map.keySet()) {
            builder.startObject(index);
            for (MappingMetaData mmd : map.get(index)) {
                builder.field(mmd.type());
                builder.map(mmd.sourceAsMap());
            }
            builder.endObject();
        }
        builder.endObject();
        os.write(builder.bytes().toBytes());
        os.flush();
        os.close();
        return mappingsFile.getAbsolutePath();
    }

    public static class Result {
        public String mappingsFile;
    }

}
