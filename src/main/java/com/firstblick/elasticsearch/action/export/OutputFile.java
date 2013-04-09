package com.firstblick.elasticsearch.action.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OutputFile extends Output {

    private Result result;
    private final String path;
    private FileOutputStream fos;
    private final boolean overwrite;

    public OutputFile(String path, boolean overwrite) {
        this.path = path;
        this.overwrite = overwrite;
    }

    @Override
    public void open() throws IOException {
        File outFile = new File(path);
        if (!overwrite && outFile.exists()){
            throw new IOException("File exists: " +  path);
        }
        fos = new FileOutputStream(outFile);
    }

    @Override
    public void close() throws IOException {
        result = new Result();
        if (fos != null) {
            fos.getChannel().force(true);
            fos.close();
            result.exit = 0;
        } else {
            result.exit = 1;
        }
        fos = null;
    }

    @Override
    public OutputStream getOutputStream() {
        return fos;
    }

    @Override
    public Result result() {
        return result;
    }
}
