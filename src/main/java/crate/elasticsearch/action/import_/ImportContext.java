package crate.elasticsearch.action.import_;

public class ImportContext {

    private boolean compression;
    private String directory;
    private String file_pattern;

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
        this.directory = directory;
    }

    public String file_pattern() {
        return file_pattern;
    }

    public void file_pattern(String file_pattern) {
        this.file_pattern = file_pattern;
    }
}
