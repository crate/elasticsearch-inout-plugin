package crate.elasticsearch.client.action.dump.index;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalClient;

import crate.elasticsearch.action.dump.index.IndexDumpAction;
import crate.elasticsearch.action.dump.index.IndexDumpRequest;
import crate.elasticsearch.action.dump.index.IndexDumpResponse;

public class IndexDumpRequestBuilder extends ActionRequestBuilder<IndexDumpRequest, IndexDumpResponse, IndexDumpRequestBuilder>{

    public IndexDumpRequestBuilder(Client client) {
        super((InternalClient) client, new IndexDumpRequest());
    }

    @Override
    protected void doExecute(ActionListener<IndexDumpResponse> listener) {
        ((Client) client).execute(IndexDumpAction.INSTANCE, request, listener);
    }

}
