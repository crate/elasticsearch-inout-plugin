package com.firstblick.elasticsearch.action.export.parser;

import com.firstblick.elasticsearch.action.export.ExportContext;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.internal.SearchContext;

/**
 * Parser for token ``output_format``. The value of the token might be a plain String or a json object.
 * Possible values for format are "json" and "delimited". If the value has been defined as json object
 * the object might contain the keys "delimiter" and "null_sequence". This information only make sense
 * if format was set to "delimited".
 *
 * <pre>
 * "output_format": "json"
 *
 * "output_format":
 *     {"delimited":
 *         {"delimiter":"\u0001",
 *         "null_sequence":"\\N"}
 *     }
 * </pre>
 */
public class ExportOutputFormatParseElement implements SearchParseElement {

    @Override
    public void parse(XContentParser parser, SearchContext context) throws Exception {
        XContentParser.Token token = parser.currentToken();
        ExportOutputFormat outputFormat = null;
        if (token.isValue()) {
            outputFormat = new ExportOutputFormat(parser.text());
        } else {
            String fieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    fieldName = parser.currentName();
                } else if (token == XContentParser.Token.START_OBJECT) {
                    if ("delimited".equals(fieldName)) {
                        outputFormat = new ExportOutputFormat(fieldName);
                    }
                } else if (token.isValue()) {
                    if ("delimiter".equals(fieldName)) {
                        outputFormat.delimiter(parser.text().toCharArray()[0]);
                    }
                    else if ("null_sequence".equals(fieldName) || "nullSequence".equals(fieldName)) {
                        outputFormat.nullSequence(parser.text());
                    }
                }
            }
        }
        ((ExportContext)context).outputFormat(outputFormat);
    }
}
