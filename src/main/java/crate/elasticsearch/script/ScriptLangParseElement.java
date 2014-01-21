package crate.elasticsearch.script;


import org.elasticsearch.common.xcontent.XContentParser;

public class ScriptLangParseElement implements ScriptParseElement {

    @Override
    public void parse(XContentParser parser, IScriptContext context)
            throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token.isValue()) {
            context.scriptLang(parser.text());
        }

    }

}
