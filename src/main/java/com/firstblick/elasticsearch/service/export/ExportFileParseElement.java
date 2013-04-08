package com.firstblick.elasticsearch.service.export;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.internal.SearchContext;

/**
 */
public class ExportFileParseElement implements SearchParseElement {

    private String lastValue;

    @Override
    public void parse(XContentParser parser, SearchContext context) throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token.isValue()) {
            ((ExportContext)context).outputFile(parser.text());
            lastValue = ((ExportContext)context).outputFile();
        }
    }

    public void reset() {
        lastValue = null;
    }

    public String getLastValue() {
        return lastValue;
    }
}
