package crate.elasticsearch.action.import_;

import java.io.IOException;
import java.util.ArrayList;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import crate.elasticsearch.import_.Importer;
import crate.elasticsearch.import_.Importer.FailedFile;
import crate.elasticsearch.import_.Importer.ImportedFile;
import crate.elasticsearch.import_.Importer.IndexedObject;

public class NodeImportResponse extends NodeOperationResponse implements ToXContent {

    private Importer.Result result;

    NodeImportResponse() {
    }

    public NodeImportResponse(DiscoveryNode discoveryNode, Importer.Result result) {
        super(discoveryNode);
        this.result = result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params)
            throws IOException {
        builder.startObject();
        builder.field(Fields.NODE_ID, this.getNode().id());
        builder.startArray(Fields.IMPORTED_FILES);
        for (ImportedFile file : result.importedFiles) {
            builder.startObject();
            builder.field(Fields.FILE_NAME, file.fileName);
            builder.field(Fields.TOOK, file.took);
            builder.startArray(Fields.ITEMS);
            for (IndexedObject obj : file.items) {
                builder.startObject();
                builder.startObject(IndexedObject.opType);
                builder.field(Fields._INDEX, obj._index);
                builder.field(Fields._TYPE, obj._type);
                builder.field(Fields._ID, obj._id);
                long version = obj._version;
                if (version != -1) {
                    builder.field(Fields._VERSION, version);
                }
                if (!obj.ok) {
                    builder.field(Fields.ERROR, obj.error);
                } else {
                    builder.field(Fields.OK, true);
                }
                builder.endObject();
                builder.endObject();
            }
            builder.endArray();
            builder.endObject();
        }
        builder.endArray();
        if (result.failedFiles.size() > 0) {
            builder.startArray(Fields.FAILED_FILES);
            for (FailedFile file : result.failedFiles) {
                builder.startObject();
                builder.field(Fields.FILE_NAME, file.fileName);
                builder.field(Fields.ERROR, file.error);
                builder.endObject();
            }
            builder.endArray();
        }
        builder.endObject();
        return builder;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int importedFiles = in.readInt();
        result = new Importer.Result();
        result.importedFiles = new ArrayList<Importer.ImportedFile>();
        for (int i = 0; i < importedFiles; i++) {
            Importer.ImportedFile file = new Importer.ImportedFile();
            file.fileName = in.readString();
            file.took = in.readLong();
            int indexedObjects = in.readInt();
            file.items = new ArrayList<Importer.IndexedObject>();
            for (int j = 0; j < indexedObjects; j++) {
                Importer.IndexedObject obj = new Importer.IndexedObject();
                obj._id = in.readString();
                obj._index = in.readString();
                obj._type = in.readString();
                obj.ok = in.readBoolean();
                obj._version = in.readLong();
                obj.error = in.readOptionalString();
                file.items.add(obj);
            }
            result.importedFiles.add(file);
        }
        int failedFiles = in.readInt();
        result.failedFiles = new ArrayList<Importer.FailedFile>();
        for (int i = 0; i < failedFiles; i++) {
            Importer.FailedFile file = new Importer.FailedFile();
            file.fileName = in.readString();
            file.error = in.readString();
            result.failedFiles.add(file);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(result.importedFiles.size());
        for (Importer.ImportedFile file : result.importedFiles) {
            out.writeString(file.fileName);
            out.writeLong(file.took);
            out.writeInt(file.items.size());
            for (Importer.IndexedObject item: file.items) {
                out.writeString(item._id);
                out.writeString(item._index);
                out.writeString(item._type);
                out.writeBoolean(item.ok);
                out.writeLong(item._version);
                out.writeOptionalString(item.error);
            }
        }
        out.writeInt(result.failedFiles.size());
        for (Importer.FailedFile file : result.failedFiles) {
            out.writeString(file.fileName);
            out.writeString(file.error);
        }
    }

    static final class Fields {
        static final XContentBuilderString FILE_NAME = new XContentBuilderString("file_name");
        static final XContentBuilderString ITEMS = new XContentBuilderString("items");
        static final XContentBuilderString _INDEX = new XContentBuilderString("_index");
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString _ID = new XContentBuilderString("_id");
        static final XContentBuilderString ERROR = new XContentBuilderString("error");
        static final XContentBuilderString OK = new XContentBuilderString("ok");
        static final XContentBuilderString TOOK = new XContentBuilderString("took");
        static final XContentBuilderString _VERSION = new XContentBuilderString("_version");
        static final XContentBuilderString MATCHES = new XContentBuilderString("matches");
        static final XContentBuilderString NODE_ID = new XContentBuilderString("node_id");
        static final XContentBuilderString IMPORTED_FILES = new XContentBuilderString("imported_files");
        static final XContentBuilderString FAILED_FILES = new XContentBuilderString("failed_files");
    }
}
