package com.firstblick.elasticsearch.action.export;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.internal.InternalSearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExportFields implements ToXContent {

    private final List<String> fields;
    private InternalSearchHit hit;
    private final List<FieldExtractor> fieldExtractors;

    abstract class FieldExtractor implements ToXContent {
    }

    class SourceFieldExtractor extends FieldExtractor {

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            BytesReference source = hit.sourceRef();
            XContentType contentType = XContentFactory.xContentType(source);
            XContentParser parser = XContentFactory.xContent(contentType)
                    .createParser(source);
            try {
                parser.nextToken();
                builder.field("_source");
                builder.copyCurrentStructure(parser);
            } finally {
                parser.close();
            }
            return builder;
        }
    }

    class HitFieldExtractor extends FieldExtractor {

        private final String fieldName;

        public HitFieldExtractor(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            SearchHitField field = hit.getFields().get(fieldName);
            if (field != null && !field.values().isEmpty()) {
                if (field.values().size() == 1) {
                    builder.field(field.name(), field.values().get(0));
                } else {
                    builder.field(field.name());
                    builder.startArray();
                    for (Object value : field.values()) {
                        builder.value(value);
                    }
                    builder.endArray();
                }
            }
            return builder;
        }
    }

    public void hit(InternalSearchHit hit) {
        this.hit = hit;
    }

    public ExportFields(List<String> fields) {
        this.fields = fields;
        this.fieldExtractors = getFieldExtractors();
    }

    private List<FieldExtractor> getFieldExtractors() {
        List<FieldExtractor> extractors = new ArrayList<FieldExtractor>(fields
                .size());
        for (String fn : fields) {
            FieldExtractor fc = null;
            if (fn.startsWith("_")) {
                if (fn.equals("_source")) {
                    fc = new SourceFieldExtractor();
                } else if (fn.equals("_id")) {
                    fc = new FieldExtractor() {
                        @Override
                        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                            return builder.field("_id", hit.getId());
                        }
                    };
                } else if (fn.equals("_type")) {
                    fc = new FieldExtractor() {
                        @Override
                        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                            return builder.field("_type", hit.getType());
                        }
                    };
                }
            } else {
                fc = new HitFieldExtractor(fn);
            }
            extractors.add(fc);
        }
        return extractors;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params
            params) throws IOException {
        builder.startObject();
        for (FieldExtractor fc : fieldExtractors) {
            fc.toXContent(builder, params);
        }
        builder.endObject();
        return builder;
    }
}

