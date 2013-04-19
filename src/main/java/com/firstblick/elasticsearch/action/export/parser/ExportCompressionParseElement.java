package com.firstblick.elasticsearch.action.export.parser;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.internal.SearchContext;

import com.firstblick.elasticsearch.action.export.ExportContext;

public class ExportCompressionParseElement implements SearchParseElement {

    @Override
    public void parse(XContentParser parser, SearchContext context)
            throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token.isValue()) {
            ((ExportContext)context).compression(parser.text());
        }
    }

}
