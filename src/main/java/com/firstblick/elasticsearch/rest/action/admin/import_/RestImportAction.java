package com.firstblick.elasticsearch.rest.action.admin.import_;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.splitTypes;

import java.io.IOException;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import com.firstblick.elasticsearch.action.import_.ImportAction;
import com.firstblick.elasticsearch.action.import_.ImportRequest;
import com.firstblick.elasticsearch.action.import_.ImportResponse;

public class RestImportAction extends BaseRestHandler {

    @Inject
    public RestImportAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_import", this);
        controller.registerHandler(POST, "/{index}/_import", this);
        controller.registerHandler(POST, "/{index}/{type}/_import", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        ImportRequest importRequest = new ImportRequest();
        importRequest.listenerThreaded(false);
        try {
            if (request.hasContent()) {
                importRequest.source(request.content(), request.contentUnsafe());
            } else {
                String source = request.param("source");
                if (source != null) {
                    importRequest.source(source);
                } else {
                    BytesReference querySource = RestActions.parseQuerySource(request);
                    if (querySource != null) {
                        importRequest.source(querySource, false);
                    }
                }
            }
            importRequest.index(request.param("index"));
            importRequest.type(request.param("type"));
        } catch (Exception e) {
            try {
                XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                channel.sendResponse(new XContentRestResponse(request, BAD_REQUEST, builder.startObject().field("error", e.getMessage()).endObject()));
            } catch (IOException e1) {
                logger.error("Failed to send failure response", e1);
            }
            return;
        }


        client.execute(ImportAction.INSTANCE, importRequest, new ActionListener<ImportResponse>() {

            public void onResponse(ImportResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    response.toXContent(builder, request);
                    channel.sendResponse(new XContentRestResponse(request, OK, builder));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }
}
