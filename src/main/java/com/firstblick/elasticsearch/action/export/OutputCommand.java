package com.firstblick.elasticsearch.action.export;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;


/**
 * Start an OS Command as a process and push strings to the process'
 * standard in. Get standard out and standard error messages when
 * process has finished.
 */
public class OutputCommand extends Output{

    private static final int BUFFER_LEN = 8192;

    private final ProcessBuilder builder;
    private PrintWriter printWriter;
    private Process process;
    private Result result;
    private StreamConsumer outputConsumer, errorConsumer;

    /**
     * Initialize the process builder with a single command.
     * @param command
     */
    public OutputCommand(String command) {
        builder = new ProcessBuilder(command);
    }

    /**
     * Initialize the process with a command list.
     * @param cmdArray
     */
    public OutputCommand(List<String> cmdArray) {
        builder = new ProcessBuilder(cmdArray);
    }

    /**
     * Start the process and prepare writing to it's standard in.
     *
     * @throws IOException
     */
    public void open() throws IOException {
        process = builder.start();
        if (process != null) {
            printWriter = new PrintWriter(process.getOutputStream());
            outputConsumer = new StreamConsumer(process.getInputStream(),
                    BUFFER_LEN);
            errorConsumer = new StreamConsumer(process.getErrorStream(),
                    BUFFER_LEN);
        }
    }

    public OutputStream getOutputStream() {
        return process.getOutputStream();
    }

    /**
     * Write a line to the process' standard in.
     *
     * @param string
     */
    public void println(String string) {
        if (printWriter != null) {
            printWriter.println(string);
        }
    }

    /**
     * Stop writing to the process' standard in and wait until the
     * process is finished and close all resources.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (printWriter != null) {
            printWriter.flush();
            printWriter.close();
        }

        if (process != null) {
            process.getOutputStream().flush();
            result = new Result();
            try {
                result.exit = process.waitFor();
            } catch (InterruptedException e) {
                result.exit = process.exitValue();
            }
            outputConsumer.waitFor();
            result.stdOut = outputConsumer.getBufferedOutput();
            errorConsumer.waitFor();
            result.stdErr = errorConsumer.getBufferedOutput();
        }
    }

    public Result result() {
        return result;
    }
}
