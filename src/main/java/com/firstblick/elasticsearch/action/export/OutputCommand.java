package com.firstblick.elasticsearch.action.export;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class OutputCommand {

    private static final int BUFFER_LEN = 8192;

    class Result {
        public int exit;
        public String stdErr;
        public String stdOut;
    }

    private final ProcessBuilder builder;
    private PrintWriter printWriter;
    private Process process;
    private Result result;
    private StreamConsumer outputConsumer, errorConsumer;

    public OutputCommand(String command) {
        builder = new ProcessBuilder(command);
    }

    public OutputCommand(List<String> cmdArray) {
        builder = new ProcessBuilder(cmdArray);
    }

    public void start() throws IOException {
        process = builder.start();
        if (process != null) {
            printWriter = new PrintWriter(process.getOutputStream());
            outputConsumer = new StreamConsumer(process.getInputStream(),
                    BUFFER_LEN);
            errorConsumer = new StreamConsumer(process.getErrorStream(),
                    BUFFER_LEN);
        }
    }

    public void println(String string) {
        if (printWriter != null) {
            printWriter.println(string);
        }
    }

    public Result end() throws IOException {
        result = new Result();
        if (printWriter != null) {
            printWriter.flush();
            printWriter.close();
        }

        if (process != null) {
            try {
                result.exit = process.waitFor();
            } catch (InterruptedException e) {
                result.exit = process.exitValue();
            }
            outputConsumer.waitFor();
            result.stdOut = outputConsumer.getBufferedOutput();
            outputConsumer.waitFor();
            result.stdErr = errorConsumer.getBufferedOutput();
        }
        return result;
    }
}
