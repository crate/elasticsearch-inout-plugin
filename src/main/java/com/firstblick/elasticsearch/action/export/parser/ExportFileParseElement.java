package com.firstblick.elasticsearch.action.export.parser;

import com.firstblick.elasticsearch.action.export.ExportContext;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.internal.SearchContext;

/**
 * Parser for token ``export_file``. The value of the token must be a String. This token MUST NOT be set together
 * with ``export_cmd``
 *
 * <pre>
 *     "export_file": "/tmp/out"
 * </pre>
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
