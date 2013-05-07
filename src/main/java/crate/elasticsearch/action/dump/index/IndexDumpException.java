package crate.elasticsearch.action.dump.index;

import java.io.IOException;

import org.elasticsearch.ElasticSearchException;

public class IndexDumpException extends ElasticSearchException {

    public IndexDumpException(String msg) {
        super(msg);
    }

    public IndexDumpException(IOException e) {
        super("Index Dump Exception:", e);
    }


}
