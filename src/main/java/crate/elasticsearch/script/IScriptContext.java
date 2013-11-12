package crate.elasticsearch.script;

import java.util.Map;

import org.elasticsearch.script.ExecutableScript;

public interface IScriptContext {

	void scriptString(String scriptString);
	
	void scriptLang(String scriptLang);
	
	void scriptParams(Map<String, Object> scriptParams);
	
	void executableScript(ExecutableScript executableScript);
	
	void executionContext(Map<String, Object> executionContext);
	
	public String scriptString();
    
    public String scriptLang();

    public Map<String, Object> scriptParams();
    
    ExecutableScript executableScript();
    
    Map<String, Object> executionContext();
    
}
