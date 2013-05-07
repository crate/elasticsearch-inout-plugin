package crate.elasticsearch.action.dump.index;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;

import crate.elasticsearch.client.action.dump.index.IndexDumpRequestBuilder;

public class IndexDumpAction extends Action<IndexDumpRequest, IndexDumpResponse, IndexDumpRequestBuilder>{

    public static final IndexDumpAction INSTANCE = new IndexDumpAction();
    public static final String NAME = "el-crate-dump-index";

    private IndexDumpAction() {
        super(NAME);
    }

    @Override
    public IndexDumpRequestBuilder newRequestBuilder(Client client) {
        return new IndexDumpRequestBuilder(client);
    }

    @Override
    public IndexDumpResponse newResponse() {
        return new IndexDumpResponse();
    }


}
