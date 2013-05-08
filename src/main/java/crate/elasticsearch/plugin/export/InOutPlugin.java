package crate.elasticsearch.plugin.export;

import crate.elasticsearch.module.dump.DumpModule;
import crate.elasticsearch.module.export.ExportModule;
import crate.elasticsearch.module.import_.ImportModule;
import crate.elasticsearch.module.restore.RestoreModule;
import crate.elasticsearch.rest.action.admin.dump.RestDumpAction;
import crate.elasticsearch.rest.action.admin.export.RestExportAction;
import crate.elasticsearch.rest.action.admin.import_.RestImportAction;
import crate.elasticsearch.rest.action.admin.restore.RestRestoreAction;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import java.util.Collection;


public class InOutPlugin extends AbstractPlugin {
    public String name() {
        return "inout";
    }

    public String description() {
        return "InOut plugin";
    }

    public void onModule(RestModule restModule) {
        restModule.addRestAction(RestExportAction.class);
        restModule.addRestAction(RestImportAction.class);
        restModule.addRestAction(RestDumpAction.class);
        restModule.addRestAction(RestRestoreAction.class);
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(ExportModule.class);
        modules.add(DumpModule.class);
        modules.add(ImportModule.class);
        modules.add(RestoreModule.class);
        return modules;
    }

}
