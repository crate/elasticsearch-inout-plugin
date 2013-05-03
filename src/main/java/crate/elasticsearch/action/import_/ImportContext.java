package crate.elasticsearch.action.import_;

import java.io.File;

public class ImportContext {

    private String nodePath;
    private boolean compression;
    private String directory;
    private String file_pattern;

    public ImportContext(String nodePath) {
        this.nodePath = nodePath;
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
            file = new File(new File(nodePath, "export"), directory);
            directory = file.getAbsolutePath();
        }
        this.directory = directory;
    }

    public String file_pattern() {
        return file_pattern;
    }

    public void file_pattern(String file_pattern) {
        this.file_pattern = file_pattern;
    }
}
