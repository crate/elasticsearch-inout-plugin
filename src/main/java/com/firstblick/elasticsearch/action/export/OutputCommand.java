package com.firstblick.elasticsearch.action.export;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


/**
 * Start an OS Command as a process and push strings to the process'
 * standard in. Get standard out and standard error messages when
 * process has finished.
 */
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
     * process is finished.
     *
     * @return a result object conatining the process exit status
     *         and the forst 8K of the process' output and error log.
     * @throws IOException
     */
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
            errorConsumer.waitFor();
            result.stdErr = errorConsumer.getBufferedOutput();
        }
        return result;
    }
}
