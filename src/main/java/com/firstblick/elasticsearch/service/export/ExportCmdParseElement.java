package com.firstblick.elasticsearch.service.export;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.internal.SearchContext;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ExportCmdParseElement implements SearchParseElement {

    @Override
    public void parse(XContentParser parser, SearchContext context) throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token.isValue()) {
            ((ExportContext)context).outputCmd(parser.text());
        } else if (token == XContentParser.Token.START_ARRAY) {
            List<String> cmds = new ArrayList<String>(4);
            while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                cmds.add(parser.text());
            }
            ((ExportContext)context).outputCmdArray(cmds);
        }
    }
}
