package com.firstblick.elasticsearch.action.import_;

import java.io.IOException;

import org.elasticsearch.action.support.nodes.NodesOperationRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Required;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class ImportRequest extends NodesOperationRequest<ImportRequest> {

    @Nullable
    protected String routing;

    @Nullable
    private String preference;

    private BytesReference source;

    private String[] types = Strings.EMPTY_ARRAY;

    private String[] indexes = Strings.EMPTY_ARRAY;

    /**
     * Constructs a new import request against the provided nodes. No nodes provided
     * means it will run against all nodes.
     */
    public ImportRequest(String... nodes) {
        super(nodes);
    }

    /**
     * The query source to execute.
     * @return
     */
    public BytesReference source() {
        return source;
    }

    @Required
    public ImportRequest source(String source) {
        return this.source(new BytesArray(source), false);
    }

    @Required
    public ImportRequest source(BytesReference source, boolean unsafe) {
        this.source = source;
        return this;
    }

    public String routing() {
        return routing;
    }

    /**
     * The routing values to control the shards that the search will be executed on.
     */
    public ImportRequest routing(String... routings) {
        this.routing = Strings.arrayToCommaDelimitedString(routings);
        return this;
    }

    public ImportRequest preference(String preference) {
        this.preference = preference;
        return this;
    }

    public String preference() {
        return preference;
    }

    public String[] types() {
        return this.types;
    }

    public void types(String... types) {
        this.types = types;
    }

    public String[] indexes() {
        return this.indexes ;
    }

    public void index(String[] indexes) {
        this.indexes = indexes;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }

}
