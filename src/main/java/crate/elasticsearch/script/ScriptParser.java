package crate.elasticsearch.script;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.collect.ImmutableMap;


public class ScriptParser {

	 private ImmutableMap<String, ScriptParseElement> scriptElementParsers;
	 
	 public ScriptParser() {
	        Map<String, ScriptParseElement> scriptElementParsers = new HashMap<String, ScriptParseElement>();
	        scriptElementParsers.put("script", new ScriptStringParseElement());
	        scriptElementParsers.put("lang", new ScriptLangParseElement());
	        scriptElementParsers.put("params", new ScriptParamsParseElement());
	        this.scriptElementParsers = ImmutableMap.copyOf(scriptElementParsers);
	    }

	public ImmutableMap<String, ScriptParseElement> scriptElementParsers() {
		return scriptElementParsers;
	}
	 
	 
}
