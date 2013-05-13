package crate.elasticsearch.action.reindex;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.fetch.FetchPhase;
import org.elasticsearch.search.fetch.explain.ExplainParseElement;
import org.elasticsearch.search.query.QueryPhase;

import crate.elasticsearch.action.searchinto.parser.AbstractSearchIntoParser;
import crate.elasticsearch.action.searchinto.parser.ISearchIntoParser;

/**
 * Parser for pay load given to _reindex action.
 */
public class ReindexParser extends AbstractSearchIntoParser implements ISearchIntoParser {

    private final ImmutableMap<String, SearchParseElement> elementParsers;

    @Inject
    public ReindexParser(QueryPhase queryPhase, FetchPhase fetchPhase) {
        Map<String, SearchParseElement> elementParsers = new HashMap<String,
                SearchParseElement>();
        elementParsers.putAll(queryPhase.parseElements());
        elementParsers.put("explain", new ExplainParseElement());
        this.elementParsers = ImmutableMap.copyOf(elementParsers);
    }

    @Override
    protected ImmutableMap<String, SearchParseElement> getElementParsers() {
        return elementParsers;
    }

}
