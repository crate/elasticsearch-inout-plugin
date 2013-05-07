package crate.elasticsearch.rest.action.admin.dump;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.splitIndices;
import static org.elasticsearch.rest.action.support.RestActions.splitTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import crate.elasticsearch.action.dump.DumpAction;
import crate.elasticsearch.action.dump.index.IndexDumpAction;
import crate.elasticsearch.action.dump.index.IndexDumpRequest;
import crate.elasticsearch.action.dump.index.IndexDumpResponse;
import crate.elasticsearch.action.export.ExportRequest;
import crate.elasticsearch.action.export.ExportResponse;
import crate.elasticsearch.rest.action.admin.export.RestExportAction;

/**
 * Rest handler for _dump endpoint
 */
public class RestDumpAction extends RestExportAction {

    @Inject
    public RestDumpAction(Settings settings, Client client, RestController controller) {
        super(settings, client, controller);
    }

    @Override
    protected Action<ExportRequest, ExportResponse, ?> action() {
        return DumpAction.INSTANCE;
    }

    @Override
    protected void registerHandlers(RestController controller) {
        controller.registerHandler(POST, "/_dump", this);
        controller.registerHandler(POST, "/{index}/_dump", this);
        controller.registerHandler(POST, "/{index}/{type}/_dump", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        try {
            final IndexDumpResponse indexDumpResponse = writeMappings(request, channel);
            if (indexDumpResponse != null) {
                ExportRequest exportRequest = prepareExportRequest(request, channel);
                client.execute(action(), exportRequest, new ActionListener<ExportResponse>() {

                    public void onResponse(ExportResponse response) {
                        try {
                            XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                            builder.startObject();
                            builder.field("metadata");
                            indexDumpResponse.toXContent(builder, request);
                            builder.field("data");
                            response.toXContent(builder, request);
                            builder.endObject();
                            channel.sendResponse(new XContentRestResponse(request, OK, builder));
                        } catch (Exception e) {
                            e.printStackTrace();
                            onFailure(e);
                        }
                    }

                    public void onFailure(Throwable e) {
                        e.printStackTrace();
                        sendFailureResponse(request, channel, e);
                    }
                });

            }
        } catch (IOException e) {
            sendFailureResponse(request, channel, e);
            return;
        }
    }

    private void sendFailureResponse(RestRequest request, RestChannel channel, Throwable e) {
        try {
            channel.sendResponse(new XContentThrowableRestResponse(request, e));
        } catch (IOException e1) {
            logger.error("Failed to send failure response", e1);
        }
    }

    private IndexDumpResponse writeMappings(RestRequest request, RestChannel channel) throws IOException {
        IndexDumpRequest idr = new IndexDumpRequest();
        if (request.hasContent()) {
            idr.source(request.content());
        } else {
            String source = request.param("source");
            if (source != null) {
                idr.source(source);
            } else {
                BytesReference querySource = RestActions.parseQuerySource(request);
                if (querySource != null) {
                    idr.source(querySource);
                }
            }
        }
        final String[] indices = splitIndices(request.param("index"));
        final Set<String> types = ImmutableSet.copyOf(splitTypes(request.param("type")));

        ClusterStateRequest clusterStateRequest = Requests.clusterStateRequest()
                .filterRoutingTable(true)
                .filterNodes(true)
                .filteredIndices(indices);

        clusterStateRequest.listenerThreaded(false);

        ClusterStateResponse clusterState = client.admin().cluster().state(clusterStateRequest).actionGet();
        MetaData metaData = clusterState.getState().metaData();

        if (indices.length == 1 && metaData.indices().isEmpty()) {
            channel.sendResponse(new XContentThrowableRestResponse(request, new IndexMissingException(new Index(indices[0]))));
            return null;
        }
        boolean hasTypes = !types.isEmpty();
        Map<String, List<MappingMetaData>> mappings = new HashMap<String,List<MappingMetaData>>();
        for (IndexMetaData indexMetaData : metaData) {
            List<MappingMetaData> indexMappings = new ArrayList<MappingMetaData>();
            for (MappingMetaData mappingMd : indexMetaData.mappings().values()) {
                if (!hasTypes || types.contains(mappingMd.type())) {
                    indexMappings.add(mappingMd);
                }
            }
            mappings.put(indexMetaData.index(), indexMappings);
        }
        idr.mappings(mappings);
        IndexDumpResponse resp = client.execute(IndexDumpAction.INSTANCE, idr).actionGet();
        if (resp.nodeFailures() != null) {
            XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
            builder.startObject();
            builder.field("metadata");
            resp.toXContent(builder, request);
            builder.endObject();
            channel.sendResponse(new XContentRestResponse(request, OK, builder));
            return null;
        }
        return resp;
    }
}
