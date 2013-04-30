package crate.elasticsearch.import_;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.util.concurrent.BaseFuture;

import crate.elasticsearch.import_.Importer.ImportCounts;

public class ImportBulkListener extends BaseFuture<ImportBulkListener> implements BulkProcessor.Listener {

    private Set<Long> executionIds = new HashSet<Long>();
    private ImportCounts counts = new ImportCounts();

    public ImportBulkListener(String fileName) {
        counts.fileName = fileName;
    }

    @Override
    public ImportBulkListener get() throws InterruptedException,
            ExecutionException {
        if (executionIds.size() == 0) {
            return this;
        }
        return super.get();
    }

    public void addFailure() {
        counts.failures++;
    }

    public ImportCounts importCounts() {
        return counts;
    }

    @Override
    public void beforeBulk(long executionId, BulkRequest request) {
        executionIds.add(executionId);
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request,
            BulkResponse response) {
        executionIds.remove(executionId);
        for (BulkItemResponse item : response.getItems()) {
            if (item.isFailed()) {
                counts.failures++;
            } else {
                counts.successes++;
            }
        }
        checkRelease();
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request,
            Throwable failure) {
        executionIds.remove(executionId);
        counts.failures += request.requests().size();
        failure.printStackTrace();
        checkRelease();
    }

    private void checkRelease() {
        if (executionIds.size() == 0) {
            this.set(this);
        }
    }

}