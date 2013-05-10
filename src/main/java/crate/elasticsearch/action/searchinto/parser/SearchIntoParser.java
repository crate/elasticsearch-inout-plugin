package crate.elasticsearch.action.searchinto.parser;

import crate.elasticsearch.action.searchinto.SearchIntoContext;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.SearchParseException;
import org.elasticsearch.search.fetch.FetchPhase;
import org.elasticsearch.search.fetch.explain.ExplainParseElement;
import org.elasticsearch.search.query.QueryPhase;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for payload given to _search_into action.
 */
public class SearchIntoParser {

    private final ImmutableMap<String, SearchParseElement> elementParsers;

    @Inject
    public SearchIntoParser(QueryPhase queryPhase, FetchPhase fetchPhase) {
        Map<String, SearchParseElement> elementParsers = new HashMap<String,
                SearchParseElement>();
        elementParsers.putAll(queryPhase.parseElements());
        elementParsers.put("fields", new FieldsParseElement());
        elementParsers.put("explain", new ExplainParseElement());
        this.elementParsers = ImmutableMap.copyOf(elementParsers);
    }

    /**
     * validate given payload
     *
     * @param context
     */
    private void validate(SearchIntoContext context) {
        if (!context.hasFieldNames()) {
            throw new SearchParseException(context, "No fields defined");
        }

        for (String field : context.fieldNames()) {
            FieldMapper mapper = context.mapperService().smartNameFieldMapper(
                    field);
            if (mapper == null && !field.equals(
                    "_version") && !field.startsWith(
                    FieldsParseElement.SCRIPT_FIELD_PREFIX)) {
                throw new SearchParseException(context,
                        "SearchInto field [" + field + "] does not exist in " +
                                "the mapping");
            }
        }

    }

    /**
     * Main method of this class to parse given payload of _search_into action
     *
     * @param context
     * @param source
     * @throws org.elasticsearch.search.SearchParseException
     *
     */
    public void parseSource(SearchIntoContext context,
            BytesReference source) throws SearchParseException {
        XContentParser parser = null;
        try {
            if (source != null) {
                parser = XContentFactory.xContent(source).createParser(source);
                XContentParser.Token token;
                while ((token = parser.nextToken()) != XContentParser.Token
                        .END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        String fieldName = parser.currentName();
                        parser.nextToken();
                        SearchParseElement element = elementParsers.get(
                                fieldName);
                        if (element == null) {
                            throw new SearchParseException(context,
                                    "No parser for element [" + fieldName +
                                            "]");
                        }
                        element.parse(parser, context);
                    } else if (token == null) {
                        break;
                    }
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
            throw new SearchParseException(context,
                    "Failed to parse source [" + sSource + "]", e);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }
}