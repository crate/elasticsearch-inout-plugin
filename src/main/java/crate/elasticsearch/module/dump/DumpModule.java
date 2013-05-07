package crate.elasticsearch.module.dump;


import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import crate.elasticsearch.action.dump.DumpAction;
import crate.elasticsearch.action.dump.TransportDumpAction;
import crate.elasticsearch.action.dump.index.IndexDumpAction;
import crate.elasticsearch.action.dump.index.IndexDumper;
import crate.elasticsearch.action.dump.index.TransportIndexDumpAction;
import crate.elasticsearch.action.dump.index.parser.IndexDumpParser;
import crate.elasticsearch.action.dump.parser.DumpParser;

public class DumpModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportDumpAction.class).asEagerSingleton();

        bind(DumpParser.class).asEagerSingleton();
        bind(IndexDumpParser.class).asEagerSingleton();
        bind(IndexDumper.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder = MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);

        transportActionsBinder.addBinding(DumpAction.INSTANCE).to(TransportDumpAction.class).asEagerSingleton();
        transportActionsBinder.addBinding(IndexDumpAction.INSTANCE).to(TransportIndexDumpAction.class).asEagerSingleton();

        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(DumpAction.NAME).toInstance(DumpAction.INSTANCE);
        actionsBinder.addBinding(IndexDumpAction.NAME).toInstance(IndexDumpAction.INSTANCE);
    }
}