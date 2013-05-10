package crate.elasticsearch.searchinto.mapping;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.index.mapper.internal.*;

import java.util.Map;

public class FieldWriter {

    private final String name;
    protected Object value;
    private BuilderWriter writer;

    private final static ImmutableMap<String, BuilderWriter> writers;

    static {
        writers = MapBuilder.<String, BuilderWriter>newMapBuilder().put(
                SourceFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.source = (Map<String, Object>) value;
            }
        }).put(IndexFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.index(value.toString());
            }
        }).put(IdFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.id(value.toString());
            }
        }).put(TypeFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.type(value.toString());
            }
        }).put(TimestampFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.timestamp(value.toString());
            }
        }).put(TTLFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.ttl((Long) value);
            }
        }).put("_version", new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.version((Long) value);
            }
        }).immutableMap();
    }

    static abstract class BuilderWriter {
        public abstract void write(IndexRequestBuilder builder, Object value);
    }

    class SourceObjectWriter extends BuilderWriter {

        private final String name;

        SourceObjectWriter(String name) {
            this.name = name;
        }

        @Override
        public void write(IndexRequestBuilder builder, Object value) {
            if (value != null) {
                builder.source.put(name, value);
            }
        }
    }

    public FieldWriter(String name) {
        this.name = name;
        initWriter();
    }

    private void initWriter() {
        if (name.startsWith("_")) {
            writer = writers.get(name);
        }
        if (writer == null) {
            writer = new SourceObjectWriter(name);
        }
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public IndexRequestBuilder toRequestBuilder(IndexRequestBuilder builder) {
        writer.write(builder, value);
        return builder;
    }


}
