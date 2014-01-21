package crate.elasticsearch.script;

import org.elasticsearch.common.xcontent.XContentParser;

public interface ScriptParseElement {

	void parse(XContentParser parser, IScriptContext context) throws Exception;
}
