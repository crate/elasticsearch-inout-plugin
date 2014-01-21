package crate.elasticsearch.action.import_.parser;

import org.elasticsearch.ElasticsearchException;

import crate.elasticsearch.action.import_.ImportContext;

public class ImportParseException extends ElasticsearchException {

    private static final long serialVersionUID = 910205724931139923L;

    public ImportParseException(ImportContext context, String msg) {
        super("Parse Failure [" + msg + "]");
    }

    public ImportParseException(ImportContext context, String msg, Throwable cause) {
        super("Parse Failure [" + msg + "]", cause);
    }
}
