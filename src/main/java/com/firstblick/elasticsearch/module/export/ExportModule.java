package com.firstblick.elasticsearch.module.export;


import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import com.firstblick.elasticsearch.action.export.ExportAction;
import com.firstblick.elasticsearch.action.export.TransportExportAction;
import com.firstblick.elasticsearch.action.export.parser.ExportParser;
import com.firstblick.elasticsearch.export.Exporter;

public class ExportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportExportAction.class).asEagerSingleton();

        bind(ExportParser.class).asEagerSingleton();
        bind(Exporter.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder = MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);

        transportActionsBinder.addBinding(ExportAction.INSTANCE).to(TransportExportAction.class).asEagerSingleton();

        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(ExportAction.NAME).toInstance(ExportAction.INSTANCE);
    }
}