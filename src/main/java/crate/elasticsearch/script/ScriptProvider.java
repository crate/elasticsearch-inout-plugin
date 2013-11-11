package crate.elasticsearch.script;

import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;

import java.util.Map;

public class ScriptProvider {

    public IScriptContext prepareContextForScriptExecution(IScriptContext context, ScriptService scriptService) {
        String scriptString = context.scriptString();
        String scriptLang = context.scriptLang();
        Map<String, Object> scriptParams = context.scriptParams();
        if (context.scriptString() != null) {
            ExecutableScript executableScript = scriptService.executable(scriptLang, scriptString, scriptParams);
            context.executableScript(executableScript);
        }
        return context;
    }
}
