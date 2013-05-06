package crate.elasticsearch.module.export;


import crate.elasticsearch.action.export.AbstractTransportExportAction;
import crate.elasticsearch.action.export.DumpAction;
import crate.elasticsearch.action.export.ExportAction;
import crate.elasticsearch.action.export.TransportDumpAction;
import crate.elasticsearch.action.export.parser.DumpParser;
import crate.elasticsearch.action.export.parser.ExportParser;
import crate.elasticsearch.action.export.parser.IExportParser;
import crate.elasticsearch.export.Exporter;
import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

public class DumpModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportDumpAction.class).asEagerSingleton();

        bind(DumpParser.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder = MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);

        transportActionsBinder.addBinding(DumpAction.INSTANCE).to(TransportDumpAction.class).asEagerSingleton();

        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(DumpAction.NAME).toInstance(DumpAction.INSTANCE);
    }
}