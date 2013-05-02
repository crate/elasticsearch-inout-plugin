package crate.elasticsearch.rest.action.admin.export;

import crate.elasticsearch.action.export.ExportAction;
import crate.elasticsearch.action.export.ExportRequest;
import crate.elasticsearch.action.export.ExportResponse;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.IgnoreIndices;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.splitTypes;

/**
 *
 */
public class RestExportAction extends BaseRestHandler {

    @Inject
    public RestExportAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_export", this);
        controller.registerHandler(POST, "/{index}/_export", this);
        controller.registerHandler(POST, "/{index}/{type}/_export", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        ExportRequest exportRequest = new ExportRequest(RestActions.splitIndices(request.param("index")));

        if (request.hasParam("ignore_indices")) {
            exportRequest.ignoreIndices(IgnoreIndices.fromString(request.param("ignore_indices")));
        }
        exportRequest.listenerThreaded(false);
        try {
            BroadcastOperationThreading operationThreading = BroadcastOperationThreading.fromString(request.param("operation_threading"), BroadcastOperationThreading.SINGLE_THREAD);
            if (operationThreading == BroadcastOperationThreading.NO_THREADS) {
                // since we don't spawn, don't allow no_threads, but change it to a single thread
                operationThreading = BroadcastOperationThreading.SINGLE_THREAD;
            }
            exportRequest.operationThreading(operationThreading);
            if (request.hasContent()) {
                exportRequest.source(request.content(), request.contentUnsafe());
            } else {
                String source = request.param("source");
                if (source != null) {
                    exportRequest.source(source);
                } else {
                    BytesReference querySource = RestActions.parseQuerySource(request);
                    if (querySource != null) {
                        exportRequest.source(querySource, false);
                    }
                }
            }
            exportRequest.routing(request.param("routing"));
            exportRequest.types(splitTypes(request.param("type")));
            exportRequest.preference(request.param("preference", "_primary"));
        } catch (Exception e) {
            try {
                XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                channel.sendResponse(new XContentRestResponse(request, BAD_REQUEST, builder.startObject().field("error", e.getMessage()).endObject()));
            } catch (IOException e1) {
                logger.error("Failed to send failure response", e1);
            }
            return;
        }

        client.execute(ExportAction.INSTANCE, exportRequest, new ActionListener<ExportResponse>() {

            public void onResponse(ExportResponse response) {
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