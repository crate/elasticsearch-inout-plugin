package crate.elasticsearch.rest.action.admin.dump;

import crate.elasticsearch.action.dump.DumpAction;
import crate.elasticsearch.rest.action.admin.export.RestExportAction;
import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;

import static org.elasticsearch.rest.RestRequest.Method.GET;
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
        controller.registerHandler(GET, "/_dump", this);
        controller.registerHandler(GET, "/{index}/_dump", this);
        controller.registerHandler(GET, "/{index}/{type}/_dump", this);
    }

}
