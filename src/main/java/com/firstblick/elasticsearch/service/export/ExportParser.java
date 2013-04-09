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
import org.elasticsearch.search.fetch.FieldsParseElement;
import org.elasticsearch.search.fetch.explain.ExplainParseElement;
import org.elasticsearch.search.query.QueryPhase;

import java.util.HashMap;
import java.util.Map;

public class ExportParser {

    private final ExportCmdParseElement exportCmdParseElement;
    private final ExportFileParseElement exportFileParseElement;

    private final ImmutableMap<String, SearchParseElement> elementParsers;


    @Inject
    public ExportParser(QueryPhase queryPhase, FetchPhase fetchPhase) {

        exportCmdParseElement = new ExportCmdParseElement();
        exportFileParseElement = new ExportFileParseElement();
        Map<String, SearchParseElement> elementParsers = new HashMap<String, SearchParseElement>();
        elementParsers.putAll(queryPhase.parseElements());
        elementParsers.put("fields", new FieldsParseElement());
        elementParsers.put("output_cmd", exportCmdParseElement);
        elementParsers.put("output_file", exportFileParseElement);
        elementParsers.put("force_override", new ExportForceOverrideParseElement());
        elementParsers.put("explain", new ExplainParseElement());
        elementParsers.put("output_format", new ExportOutputFormatParseElement());
        this.elementParsers = ImmutableMap.copyOf(elementParsers);
    }

    /**
     * validates output_cmd and output_file to make sure exactly one of both has been defined
     *
     * @param context
     */
    private void validateOutputCmd(ExportContext context) {
        if (exportCmdParseElement.getLastValue() != null && exportFileParseElement.getLastValue() != null) {
            throw new SearchParseException(context, "Concurrent definition of 'output_cmd' and 'output_file'");
        } else if (exportCmdParseElement.getLastValue() == null && exportFileParseElement.getLastValue() == null){
            throw new SearchParseException(context, "'output_cmd' or 'output_file' has not been defined");
        }
    }

    private void validateFields(ExportContext context) {
        if (!context.hasFieldNames()) {
            throw new SearchParseException(context, "No export fields defined");
        }
        for (String field : context.fieldNames()) {
            if (context.mapperService().name(field) == null) {
                throw new SearchParseException(context, "Export field [" + field + "] does not exist in the mapping");
            }
        }
    }

    /**
     * reset custom parseElements
     */
    private void reset() {
        exportCmdParseElement.reset();
        exportFileParseElement.reset();
    }

    private void validate(ExportContext context) {
        validateOutputCmd(context);
        validateFields(context);
    }

    public void parseSource(ExportContext context, BytesReference source) throws SearchParseException {
        // nothing to parse...
        if (source == null || source.length() == 0) {
            return;
        }
        reset();
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
            validate(context);
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