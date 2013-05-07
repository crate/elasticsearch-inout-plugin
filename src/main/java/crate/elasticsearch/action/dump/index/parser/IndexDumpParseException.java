package crate.elasticsearch.action.dump.index.parser;

import org.elasticsearch.ElasticSearchException;

import crate.elasticsearch.action.dump.index.IndexDumpContext;

public class IndexDumpParseException extends ElasticSearchException {

    private static final long serialVersionUID = 3950529860602924513L;

    public IndexDumpParseException(IndexDumpContext context, String msg) {
        super("Parse Failure [" + msg + "]");
    }

    public IndexDumpParseException(IndexDumpContext context, String msg, Throwable cause) {
        super("Parse Failure [" + msg + "]", cause);
        cause.printStackTrace();
    }
}
