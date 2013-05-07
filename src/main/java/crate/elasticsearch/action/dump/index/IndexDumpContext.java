package crate.elasticsearch.action.dump.index;

import java.io.File;


public class IndexDumpContext {

    private boolean forceOverride = false;
    private String directory;
    private String nodePath;

    public IndexDumpContext(String nodePath) {
        this.nodePath = nodePath;
    }

    public boolean forceOverride() {
        return forceOverride;
    }

    public void forceOverride(boolean forceOverride) {
        this.forceOverride = forceOverride;
    }

    public String directory() {
        return directory;
    }

    public void directory(String directory) {
        File outFile = new File(directory);
        if (!outFile.isAbsolute() && nodePath != null) {
            directory = new File(nodePath, directory).getAbsolutePath();
        }
        this.directory = directory;
    }

}
