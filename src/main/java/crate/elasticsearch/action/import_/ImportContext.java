package crate.elasticsearch.action.import_;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.elasticsearch.script.ExecutableScript;

import crate.elasticsearch.script.IScriptContext;

public class ImportContext implements IScriptContext{

    private String nodePath;
    private boolean compression;
    private String directory;
    private Pattern file_pattern;
    private boolean mappings = false;
    private boolean settings = false;
    private String scriptString;
    private String scriptLang;
    private Map<String, Object> scriptParams;
    private Map<String, Object> executionContext;
    private ExecutableScript executableScript;
    
	public ImportContext(String nodePath) {
		super();
        this.nodePath = nodePath;
        this.executionContext = new HashMap<String, Object>();
    }

	public boolean compression() {
        return compression;
    }

    public void compression(boolean compression) {
        this.compression = compression;
    }

    public String directory() {
        return directory;
    }

    public void directory(String directory) {
        File file = new File(directory);
        if (!file.isAbsolute() && nodePath != null) {
            file = new File(nodePath, directory);
            directory = file.getAbsolutePath();
        }
        this.directory = directory;
    }

    public Pattern file_pattern() {
        return file_pattern;
    }

    public void file_pattern(Pattern file_pattern) {
        this.file_pattern = file_pattern;
    }

    public boolean mappings() {
        return mappings;
    }

    public void mappings(boolean mappings) {
        this.mappings = mappings;
    }

    public boolean settings() {
        return settings;
    }

    public void settings(boolean settings) {
        this.settings = settings;
    }
    


    @Override
    public String scriptString() {
        return scriptString;
    }

    @Override
    public void scriptString(String scriptString) {
        this.scriptString = scriptString;
    }

    @Override
    public String scriptLang() {
        return scriptLang;
    }

    @Override
    public void scriptLang(String scriptLang) {
        this.scriptLang = scriptLang;
    }

    @Override
    public Map<String, Object> scriptParams() {
        return scriptParams;
    }

    @Override
    public void scriptParams(Map<String, Object> scriptParams) {
        this.scriptParams = scriptParams;
    }

   
    @Override
    public void executableScript(ExecutableScript executableScript) {
        this.executableScript = executableScript;
    }

   
    @Override
    public void executionContext(Map<String, Object> executionContext) {
        this.executionContext = executionContext;
    }

    @Override
	public Map<String, Object> executionContext() {
		return executionContext;
	}

    @Override
	public ExecutableScript executableScript() {
		return executableScript;
	}
    
}
