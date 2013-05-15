package crate.elasticsearch.searchinto;

import crate.elasticsearch.action.searchinto.SearchIntoContext;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.BaseFuture;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.script.ScriptFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.version.VersionFetchSubPhase;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public class BulkWriterCollector extends WriterCollector {


    private static final ESLogger logger = Loggers.getLogger(
            BulkWriterCollector.class);

    private Client client;
    private BulkProcessor bulkProcessor;

    private final AtomicLong startedDocs = new AtomicLong(0);
    private final AtomicLong succeededDocs = new AtomicLong(0);

    public static final String NAME = "index";
    private BulkListener bulkListener;

    public BulkWriterCollector() {
        super();
    }

    @Inject
    public BulkWriterCollector(@Assisted SearchIntoContext context,
            Client client, ScriptFieldsFetchSubPhase scriptFieldsPhase,
            VersionFetchSubPhase versionFetchSubPhase) {
        super(context,
                new FetchSubPhase[]{versionFetchSubPhase, scriptFieldsPhase});
        this.client = client;
    }

    /**
     * determine which client to use depending on context
     *
     * @return injected client or TransportClient
     */
    private Client getClient() {

        SearchIntoContext ctx = (SearchIntoContext)context;
        if (ctx.targetNodes().isEmpty()) {
            return client;
        } else {
            ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
            builder.put("config.ignore_system_properties", true);
            builder.put("client.transport.sniff", true);
            builder.put("client.transport.ignore_cluster_name", true);
            Client c = new TransportClient(builder, false);
            for (InetSocketTransportAddress address : ctx.targetNodes()) {
                ((TransportClient)c).addTransportAddress(address);
            }
            return c;
        }
    }

    @Override
    public void open() throws WriterException {

        bulkListener = new BulkListener();
        bulkProcessor = BulkProcessor.builder(getClient(),
                bulkListener).setBulkActions(1000).setBulkSize(
                new ByteSizeValue(5, ByteSizeUnit.MB)).setFlushInterval(
                TimeValue.timeValueSeconds(5)).setConcurrentRequests(
                1).build();
    }

    private class BulkListener extends BaseFuture<BulkListener> implements
            BulkProcessor.Listener {

        private AtomicLong bulksInProgress = new AtomicLong();

        @Override
        public BulkListener get() throws InterruptedException,
                ExecutionException {
            if (bulksInProgress.get() == 0) {
                return this;
            }
            return super.get();
        }

        @Override
        public void beforeBulk(long executionId, BulkRequest request) {
            logger.debug("beforeBulk {}", executionId);
            bulksInProgress.incrementAndGet();
            startedDocs.addAndGet(request.numberOfActions());
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request,
                BulkResponse response) {
            logger.debug("afterBulk {} failures:{}", executionId,
                    response.hasFailures());
            if (response.hasFailures()) {
                long succeeded = 0;
                for (Iterator<BulkItemResponse> i = response.iterator(); i
                        .hasNext(); ) {
                    if (!i.next().isFailed()) {
                        succeeded++;
                    }
                }
                if (succeeded > 0) {
                    succeededDocs.addAndGet(succeeded);
                }
            } else {
                succeededDocs.addAndGet(request.numberOfActions());
            }
            bulkProcessed();
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request,
                Throwable failure) {
            logger.debug("afterBulk failed {} {}", executionId, failure);

            bulkProcessed();
        }

        private void bulkProcessed() {
            if (bulksInProgress.decrementAndGet() == 0) {
                this.set(this);
            }
        }

    }


    @Override
    public void close() throws WriterException {
        logger.debug("close()");
        bulkProcessor.close();
        try {
            bulkListener.get();
        } catch (InterruptedException e) {
            throw new WriterException(context,
                    "BulkListener interrupted on " + "close", e);

        } catch (ExecutionException e) {
            throw new WriterException(context,
                    "BulkListener execution " + "failed on close", e);
        }
    }

    @Override
    public WriterResult getResult() {
        logger.debug("getResult {}, {}", succeededDocs.get(),
                startedDocs.get());
        WriterResult res = new WriterResult();
        res.setSucceededWrites(succeededDocs.get());
        res.setTotalWrites(startedDocs.get());
        res.setFailedWrites(res.getTotalWrites() - res.getSucceededWrites());
        return res;
    }

    @Override
    public void collectHit(SearchHit hit) throws IOException {
        mappedFields.hit(hit);
        IndexRequest req = mappedFields.newIndexRequest();
        bulkProcessor.add(req);

    }
}
