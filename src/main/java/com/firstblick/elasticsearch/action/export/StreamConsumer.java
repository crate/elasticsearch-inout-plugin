package com.firstblick.elasticsearch.action.export;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamConsumer {

    private final StreamConsumerImpl impl;
    private Thread thread;

    public StreamConsumer(InputStream inputStream, int bufferSize) {
        impl = new StreamConsumerImpl(inputStream, bufferSize);
        thread = new Thread(impl);
        thread.start();
    }

    public String getBufferedOutput() {
        return impl.getOutput();
    }

    public void waitFor() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final class StreamConsumerImpl implements Runnable {

        private final int bufferSize;
        private final StringBuffer collectedOutput = new StringBuffer();
        private InputStream inputStream;

        private StreamConsumerImpl(InputStream inputStream, int bufferSize) {
            this.bufferSize = bufferSize;
            this.inputStream = inputStream;
        }

        public void run() {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    inputStream));
            String line;
            try {
                do {
                    line = bufferedReader.readLine();
                    if (line != null && collectedOutput.length() < bufferSize) {
                        collectedOutput.append(line + "\n");
                    }
                } while (line != null);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String getOutput() {
            return collectedOutput.toString();
        }
    }
}
