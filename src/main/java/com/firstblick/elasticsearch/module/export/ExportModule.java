package com.firstblick.elasticsearch.module.export;


import com.firstblick.elasticsearch.action.export.ExportAction;
import com.firstblick.elasticsearch.action.export.TransportExportAction;
import com.firstblick.elasticsearch.service.export.ExportParser;
import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.count.CountAction;
import org.elasticsearch.action.count.TransportCountAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

public class ExportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportExportAction.class).asEagerSingleton();

        bind(ExportParser.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder =
                MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);

        transportActionsBinder.addBinding(ExportAction.INSTANCE).to(TransportExportAction.class).asEagerSingleton();

        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(ExportAction.NAME).toInstance(ExportAction.INSTANCE);
    }
}