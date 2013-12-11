package crate.elasticsearch.script;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.xcontent.XContentParser;

public class ScriptStringParseElement implements ScriptParseElement {

	protected final ESLogger logger = ESLoggerFactory.getLogger(this.getClass().getName());

	
    @Override
    public void parse(XContentParser parser, IScriptContext context)
            throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token.isValue()) {
        	String content = parser.text();
        	logger.info("Added script into the context: " + content);
            context.scriptString(content);
        }

    }

}
