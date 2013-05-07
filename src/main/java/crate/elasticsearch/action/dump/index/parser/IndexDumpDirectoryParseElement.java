package crate.elasticsearch.action.dump.index.parser;

import org.elasticsearch.common.xcontent.XContentParser;

import crate.elasticsearch.action.dump.index.IndexDumpContext;

public class IndexDumpDirectoryParseElement implements IndexDumpParseElement {

    @Override
    public void parse(XContentParser parser, IndexDumpContext context)
            throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token.isValue()) {
            context.directory(parser.text());
        }
    }
}
