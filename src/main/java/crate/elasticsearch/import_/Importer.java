package crate.elasticsearch.import_;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
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

    private Client client;
    private Injector injector;

    private final ByteSizeValue bulkByteSize = new ByteSizeValue(5, ByteSizeUnit.MB);
    private final TimeValue flushInterval = TimeValue.timeValueSeconds(5);
    private final int concurrentRequests = 4;

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
        int bulkSize = request.bulkSize();
        Result result = new Result();
        Date start = new Date();
        File dir = new File(context.directory());
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                ImportCounts counts = handleFile(file, index, type, bulkSize);
                if (counts != null) {
                    result.importCounts.add(counts);
                }
            }
        }
        result.took = new Date().getTime() - start.getTime();
        return result;
    }

    private ImportCounts handleFile(File file, String index, String type, int bulkSize) {
        if (file.isFile() && file.canRead()) {
            ImportBulkListener bulkListener = new ImportBulkListener(file.getName());
            BulkProcessor bulkProcessor = BulkProcessor.builder(client, bulkListener)
                    .setBulkActions(bulkSize)
                    .setBulkSize(bulkByteSize)
                    .setFlushInterval(flushInterval)
                    .setConcurrentRequests(concurrentRequests)
                    .build();
            try {
                BufferedReader r = new BufferedReader(new FileReader(file));
                String line;
                while ((line = r.readLine()) != null) {
                    IndexRequest indexRequest;
                    try {
                        indexRequest = parseObject(line);
                    } catch (ObjectImportException e) {
                        bulkListener.addFailure();
                        continue;
                    }
                    if (indexRequest != null) {
                        indexRequest.opType(OpType.INDEX);
                        if (index != null) {
                            indexRequest.index(index);
                        }
                        if (type != null) {
                            indexRequest.type(type);
                        }
                        if (indexRequest.type() != null && indexRequest.index() != null) {
                            bulkProcessor.add(indexRequest);
                        } else {
                            bulkListener.addFailure();
                        }
                    } else {
                        bulkListener.addInvalid();
                    }
                }
            } catch (FileNotFoundException e) {
                // Ignore not existing files, actually they should exist, as they are filtered before.
            } catch (IOException e) {
            } finally {
                bulkProcessor.close();
            }
            try {
                bulkListener.get();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            }
            return bulkListener.importCounts();
        }
        return null;
    }

    private IndexRequest parseObject(String line) throws ObjectImportException {
        XContentParser parser = null;
        try {
            IndexRequest indexRequest = new IndexRequest();
            parser = XContentFactory.xContent(line.getBytes()).createParser(line.getBytes());
            Token token;
            XContentBuilder sourceBuilder = XContentFactory.contentBuilder(XContentType.JSON);
            long ttl = 0;
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
                    } else if (fieldName.equals(TimestampFieldMapper.NAME) && token == Token.VALUE_NUMBER) {
                        indexRequest.timestamp(String.valueOf(parser.longValue()));
                    } else if (fieldName.equals(TTLFieldMapper.NAME) && token == Token.VALUE_NUMBER) {
                        ttl = parser.longValue();
                    } else if (fieldName.equals("_version") && token == Token.VALUE_NUMBER) {
                        indexRequest.version(parser.longValue());
                        indexRequest.versionType(VersionType.EXTERNAL);
                    } else if (fieldName.equals(SourceFieldMapper.NAME) && token == Token.START_OBJECT) {
                        sourceBuilder.copyCurrentStructure(parser);
                    }
                } else if (token == null) {
                    break;
                }
            }
            if (ttl > 0) {
                String ts = indexRequest.timestamp();
                long start;
                if (ts != null) {
                    start = Long.valueOf(ts);
                } else {
                    start = new Date().getTime();
                }
                ttl = ttl - start;
                if (ttl > 0) {
                    indexRequest.ttl(ttl);
                } else {
                    // object is invalid, do not import
                    return null;
                }
            }
            indexRequest.source(sourceBuilder);
            return indexRequest;
        } catch (ElasticSearchParseException e) {
            throw new ObjectImportException(e);
        } catch (IOException e) {
            throw new ObjectImportException(e);
        }
    }

    class ObjectImportException extends ElasticSearchException {

        private static final long serialVersionUID = 2405764408378929056L;

        public ObjectImportException(Throwable cause) {
            super("Object could not be imported.", cause);
        }
   }

    public static class Result {
        public List<ImportCounts> importCounts = new ArrayList<Importer.ImportCounts>();
        public long took;
    }

    public static class ImportCounts {
        public String fileName;
        public int successes = 0;
        public int failures = 0;
        public int invalid = 0;
    }

}
