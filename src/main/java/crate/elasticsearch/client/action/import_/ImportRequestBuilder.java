package crate.elasticsearch.client.action.import_;

import crate.elasticsearch.action.import_.ImportAction;
import crate.elasticsearch.action.import_.ImportRequest;
import crate.elasticsearch.action.import_.ImportResponse;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalClient;

public class ImportRequestBuilder extends ActionRequestBuilder<ImportRequest, ImportResponse, ImportRequestBuilder> {

    public ImportRequestBuilder(Client client) {
        super((InternalClient) client, new ImportRequest());
    }

    @Override
    protected void doExecute(ActionListener<ImportResponse> listener) {
        ((Client) client).execute(ImportAction.INSTANCE, request, listener);
    }
}
