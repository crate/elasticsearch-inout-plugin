package com.firstblick.elasticsearch.module.imports;

import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import com.firstblick.elasticsearch.action.imports.ImportAction;
import com.firstblick.elasticsearch.action.imports.TransportImportAction;

public class ImportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportImportAction.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder = MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);
        transportActionsBinder.addBinding(ImportAction.INSTANCE).to(TransportImportAction.class).asEagerSingleton();

        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(ImportAction.NAME).toInstance(ImportAction.INSTANCE);

    }

}
