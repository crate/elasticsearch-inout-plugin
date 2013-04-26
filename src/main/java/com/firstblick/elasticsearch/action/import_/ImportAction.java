package com.firstblick.elasticsearch.action.import_;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;

import com.firstblick.elasticsearch.client.action.import_.ImportRequestBuilder;

public class ImportAction extends Action<ImportRequest, ImportResponse, ImportRequestBuilder> {

    public static final ImportAction INSTANCE = new ImportAction();
    public static final String NAME = "el-crate-import";

    private ImportAction() {
        super(NAME);
    }

    @Override
    public ImportResponse newResponse() {
        return new ImportResponse();
    }

    @Override
    public ImportRequestBuilder newRequestBuilder(Client client) {
        return new ImportRequestBuilder(client);
    }
}
