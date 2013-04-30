package crate.elasticsearch.import_;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.mapper.internal.IdFieldMapper;
import org.elasticsearch.index.mapper.internal.IndexFieldMapper;
import org.elasticsearch.index.mapper.internal.RoutingFieldMapper;
import org.elasticsearch.index.mapper.internal.SourceFieldMapper;
import org.elasticsearch.index.mapper.internal.TTLFieldMapper;
import org.elasticsearch.index.mapper.internal.TimestampFieldMapper;
import org.elasticsearch.index.mapper.internal.TypeFieldMapper;

import crate.elasticsearch.action.import_.ImportContext;
import crate.elasticsearch.action.import_.NodeImportRequest;

public class Importer {

    public static class IndexedObject {
        public final static String opType = "index";
        public String _index;
        public String _type;
        public String _id;
        public long  _version;
        public boolean ok;
        public String error;
    }

    public static class HandledFile {
        public String fileName;
    }

    public static class ImportedFile extends HandledFile {
        public long took;
        public List<IndexedObject> items;
    }

    public static class FailedFile extends HandledFile {
        public String error;
    }

    public static class Result {
        public List<ImportedFile> importedFiles = new ArrayList<Importer.ImportedFile>();
        public List<FailedFile> failedFiles = new ArrayList<Importer.FailedFile>();
    }

    private Client client;
    private Injector injector;

    @Inject
    public Importer(Injector injector) {
        this.injector = injector;
    }

    public Result execute(ImportContext context, NodeImportRequest request) {
        if (this.client == null) {
            // Inject here to avoid injection loop in constructor
            this.client = injector.getInstance(Client.class);
        }
        String index = request.index();
        String type = request.type();
        Result result = new Result();
        File dir = new File(context.directory());
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                HandledFile handledFile = handleFile(file, index, type);
                if (handledFile != null) {
                    if (handledFile instanceof ImportedFile) {
                        result.importedFiles.add((ImportedFile) handledFile);
                    } else if (handledFile instanceof FailedFile) {
                        result.failedFiles.add((FailedFile) handledFile);
                    }
                }
            }
        }
        // Handle single file too?
        return result;
    }

    private HandledFile handleFile(File file, String index, String type) {
        if (file.isFile() && file.canRead()) {
            BulkRequest bulkRequest = Requests.bulkRequest();
            bulkRequest.listenerThreaded(false);
            boolean added = false;
            try {
                BufferedReader r = new BufferedReader(new FileReader(file));
                String line;
                while ((line = r.readLine()) != null) {
                    IndexRequest indexRequest = parseObject(line);
                    if (indexRequest != null) {
                        added = true;
                        indexRequest.opType(OpType.INDEX);
                        if (index != null) {
                            indexRequest.index(index);
                        }
                        if (type != null) {
                            indexRequest.type(type);
                        }
                        bulkRequest.add(indexRequest);
                    }
                }
            } catch (FileNotFoundException e) {
                // Ignore not existing files, actually they should exist, as they are filtered before.
            } catch (IOException e) {
                FailedFile failedFile = new FailedFile();
                failedFile.fileName = file.getName();
                failedFile.error = e.getMessage();
            }
            if (added) {
                try {
                    BulkResponse response = client.bulk(bulkRequest).actionGet();
                    ImportedFile importedFile = new ImportedFile();
                    importedFile.fileName = file.getName();
                    importedFile.took = response.getTookInMillis();
                    importedFile.items = new ArrayList<IndexedObject>();
                    for (BulkItemResponse item : response.getItems()) {
                        IndexedObject obj = new IndexedObject();
                        obj._id = item.getId();
                        obj._index = item.getIndex();
                        obj._type = item.getType();
                        obj.ok = !item.isFailed();
                        obj._version = item.getVersion();
                        obj.error = item.getFailureMessage();
                        importedFile.items.add(obj);
                    }
                    return importedFile;
                } catch (ElasticSearchException e) {
                    e.printStackTrace();
                    FailedFile failedFile = new FailedFile();
                    failedFile.fileName = file.getName();
                    failedFile.error = e.getMessage();
                    return failedFile;
                }
            }
        }
        return null;
    }

    private IndexRequest parseObject(String line) {
        XContentParser parser = null;
        try {
            IndexRequest indexRequest = new IndexRequest();
            parser = XContentFactory.xContent(line.getBytes()).createParser(line.getBytes());
            Token token;
            XContentBuilder sourceBuilder = XContentFactory.contentBuilder(XContentType.JSON);
            while ((token = parser.nextToken()) != Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    token = parser.nextToken();
                    if (fieldName.equals(IdFieldMapper.NAME) && token == Token.VALUE_STRING) {
                        indexRequest.id(parser.text());
                    } else if (fieldName.equals(IndexFieldMapper.NAME) && token == Token.VALUE_STRING) {
                        indexRequest.index(parser.text());
                    } else if (fieldName.equals(TypeFieldMapper.NAME) && token == Token.VALUE_STRING) {
                        indexRequest.type(parser.text());
                    } else if (fieldName.equals(RoutingFieldMapper.NAME) && token == Token.VALUE_STRING) {
                        indexRequest.routing(parser.text());
                    } else if (fieldName.equals(TimestampFieldMapper.NAME) && token == Token.VALUE_STRING) {
                        indexRequest.timestamp(parser.text());
                    } else if (fieldName.equals(TTLFieldMapper.NAME) && token == Token.VALUE_NUMBER) {
                        indexRequest.ttl(new Date().getTime() - parser.longValue());
                    } else if (fieldName.equals("_version") && token == Token.VALUE_NUMBER) {
                        indexRequest.version(parser.longValue());
                        indexRequest.versionType(VersionType.EXTERNAL);
                    } else if (fieldName.equals(SourceFieldMapper.NAME) && token == Token.START_OBJECT) {
                        sourceBuilder.copyCurrentStructure(parser);
                    } else {
                        // what to do with other fields?
                    }
                } else if (token == null) {
                    break;
                }
            }
            indexRequest.source(sourceBuilder);
            return indexRequest;
        } catch (IOException e) {
        }
        return null;
    }
}
