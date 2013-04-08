package com.firstblick.elasticsearch.plugin.export;

import com.firstblick.elasticsearch.module.export.ExportModule;
import com.firstblick.elasticsearch.rest.action.admin.export.RestExportAction;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import java.util.Collection;


public class ExportPlugin extends AbstractPlugin {
    public String name() {
        return "export";
    }

    public String description() {
        return "Export plugin";
    }

    public void onModule(RestModule restModule) {
        restModule.addRestAction(RestExportAction.class);
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(ExportModule.class);
        return modules;
    }

}
