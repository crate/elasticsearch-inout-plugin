package com.firstblick.elasticsearch.service.export;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.SearchParseException;
import org.elasticsearch.search.fetch.FetchPhase;
import org.elasticsearch.search.query.QueryPhase;

import java.util.HashMap;
import java.util.Map;

public class ExportParser {

    private final ImmutableMap<String, SearchParseElement> elementParsers;


    @Inject
    public ExportParser(QueryPhase queryPhase, FetchPhase fetchPhase) {

        Map<String, SearchParseElement> elementParsers = new HashMap<String, SearchParseElement>();
        elementParsers.putAll(queryPhase.parseElements());
        elementParsers.putAll(fetchPhase.parseElements());
        elementParsers.put("output_cmd", new ExportCmdParseElement());
        elementParsers.put("output_file", new ExportFileParseElement());
        this.elementParsers = ImmutableMap.copyOf(elementParsers);
    }

    public void parseSource(ExportContext context, BytesReference source) throws SearchParseException {
        // nothing to parse...
        if (source == null || source.length() == 0) {
            return;
        }
        XContentParser parser = null;
        try {
            parser = XContentFactory.xContent(source).createParser(source);
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    SearchParseElement element = elementParsers.get(fieldName);
                    if (element == null) {
                        throw new SearchParseException(context, "No parser for element [" + fieldName + "]");
                    }
                    element.parse(parser, context);
                } else if (token == null) {
                    break;
                }
            }
        } catch (Exception e) {
            String sSource = "_na_";
            try {
                sSource = XContentHelper.convertToJson(source, false);
            } catch (Throwable e1) {
                // ignore
            }
            throw new SearchParseException(context, "Failed to parse source [" + sSource + "]", e);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }
}