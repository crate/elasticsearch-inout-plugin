package crate.elasticsearch.action.dump.index.parser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.SearchParseException;

import crate.elasticsearch.action.dump.index.IndexDumpContext;
import crate.elasticsearch.action.dump.parser.DumpParser;

public class IndexDumpParser {

    private final ImmutableMap<String, IndexDumpParseElement> elementParsers;
    private final IndexDumpDirectoryParseElement directoryParseElement = new IndexDumpDirectoryParseElement();

    public IndexDumpParser() {
        Map<String, IndexDumpParseElement> elementParsers = new HashMap<String, IndexDumpParseElement>();
        elementParsers.put("force_overwrite", new IndexDumpForceOverwriteParseElement());
        elementParsers.put("directory", directoryParseElement);
        this.elementParsers = ImmutableMap.copyOf(elementParsers);
    }

    public void parseSource(IndexDumpContext context, BytesReference source) throws SearchParseException {
        XContentParser parser = null;
        try {
            if (source != null && source.length() > 0) {
                parser = XContentFactory.xContent(source).createParser(source);
                XContentParser.Token token;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        String fieldName = parser.currentName();
                        parser.nextToken();
                        IndexDumpParseElement element = elementParsers.get(fieldName);
                        if (element != null) {
                            element.parse(parser, context);
                        }
                    } else if (token == null) {
                        break;
                    }
                }
            }
            if (context.directory() == null) {
                context.directory(DumpParser.DEFAULT_DIR);
                this.ensureDefaultDirectory(context);
            }
        } catch (Exception e) {
            String sSource = "_na_";
            try {
                sSource = XContentHelper.convertToJson(source, false);
            } catch (Throwable e1) {
                // ignore
            }
            throw new IndexDumpParseException(context, "Failed to parse source [" + sSource + "]", e);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    /**
     * create default dump directory if it does not exist
     *
     * @param context
     */
    private void ensureDefaultDirectory(IndexDumpContext context) {
        File dumpDir = new File(context.directory());
        if (!dumpDir.exists()) {
            dumpDir.mkdir();
        }
    }
}
