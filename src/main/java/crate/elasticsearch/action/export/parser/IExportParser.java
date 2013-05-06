package crate.elasticsearch.action.export.parser;

import crate.elasticsearch.action.export.ExportContext;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.search.SearchParseException;

/**
 * Created with IntelliJ IDEA.
 * User: bernd
 * Date: 03.05.13
 * Time: 15:36
 * To change this template use File | Settings | File Templates.
 */
public interface IExportParser {

    public void parseSource(ExportContext context, BytesReference source) throws SearchParseException;
}
