package com.firstblick.elasticsearch.service.export;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.internal.SearchContext;

/**
 */
public class ExportForceOverrideParseElement implements SearchParseElement {

    @Override
    public void parse(XContentParser parser, SearchContext context) throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token.isValue()) {
            ((ExportContext)context).forceOverride(parser.booleanValue());
        }
    }
}
