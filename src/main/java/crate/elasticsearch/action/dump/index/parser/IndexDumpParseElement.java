package crate.elasticsearch.action.dump.index.parser;

import org.elasticsearch.common.xcontent.XContentParser;

import crate.elasticsearch.action.dump.index.IndexDumpContext;

public interface IndexDumpParseElement {

    void parse(XContentParser parser, IndexDumpContext context) throws Exception;

}
