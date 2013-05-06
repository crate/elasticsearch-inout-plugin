package crate.elasticsearch.rest.action.admin.dump;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.action.support.RestActions.splitIndices;
import static org.elasticsearch.rest.action.support.RestActions.splitTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentThrowableRestResponse;

import crate.elasticsearch.action.export.DumpAction;
import crate.elasticsearch.action.export.ExportRequest;
import crate.elasticsearch.action.export.ExportResponse;
import crate.elasticsearch.action.export.IndexCreationRequest;
import crate.elasticsearch.rest.action.admin.export.RestExportAction;

/**
 * Created with IntelliJ IDEA.
 * User: bernd
 * Date: 03.05.13
 * Time: 10:05
 * To change this template use File | Settings | File Templates.
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
    public void handleRequest(RestRequest request, RestChannel channel) {
        try {
            IndexCreationRequest icr = new IndexCreationRequest();
            getMappings(request, channel, icr);
        } catch (IOException e) {
            try {
                channel.sendResponse(new XContentThrowableRestResponse(request, e));
            } catch (IOException e1) {
                logger.error("Failed to send failure response", e1);
            }
            return;
        }

        super.handleRequest(request, channel);
    }

    private boolean getMappings(RestRequest request, RestChannel channel, IndexCreationRequest icr) throws IOException {
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
            return false;
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
        icr.mappings(mappings);
        return true;
    }
}
