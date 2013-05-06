package crate.elasticsearch.rest.action.admin.dump;

import crate.elasticsearch.action.export.DumpAction;
import crate.elasticsearch.action.export.ExportAction;
import crate.elasticsearch.action.export.parser.ExportParserFactory;
import crate.elasticsearch.rest.action.admin.export.RestExportAction;
import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.support.RestActions;

import static org.elasticsearch.rest.RestRequest.Method.POST;

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
    protected Action action() {
        return DumpAction.INSTANCE;
    }

    @Override
    protected void registerHandlers(RestController controller) {
        controller.registerHandler(POST, "/_dump", this);
        controller.registerHandler(POST, "/{index}/_dump", this);
        controller.registerHandler(POST, "/{index}/{type}/_dump", this);
    }

}
