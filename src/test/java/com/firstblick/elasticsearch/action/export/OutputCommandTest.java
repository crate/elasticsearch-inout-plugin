package com.firstblick.elasticsearch.action.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.firstblick.elasticsearch.action.export.OutputCommand.Result;

/**
 * Tests for the @OutputCommand class. These tests currently call
 * UNIX commands and are only executable on Linux/MacOS Systems.
 */
public class OutputCommandTest {

    @Test
    public void testWithoutStart() {
        // Create a 'cat' output command. The cat command puts
        // the standard in strings to the standard out
        OutputCommand outputCommand = new OutputCommand("cat");

        // Before starting the println method has no effect
        outputCommand.println("nothing");

        // The end() method returns null if the output command has not started
        Result result = null;
        try {
            result = outputCommand.end();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Method end() failed due to IOException");
        }
        assertNull(result);
    }

    @Test
    public void testErrorCommand() {
        OutputCommand outputCommand = new OutputCommand("_notexistingcommand");

        // Start the process
        try {
            outputCommand.start();
        } catch (IOException e) {
            // The command does not exist and should raise an IOException
            return;
        }
        fail("Test should raise IOException");
    }

    @Test
    public void testSingleCommand() {
        // Create a 'cat' output command. The cat command puts
        // the standard in strings to the standard out
        OutputCommand outputCommand = new OutputCommand("cat");

        // Start the process
        try {
            outputCommand.start();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Method start() failed due to IOException");
        }

        // Add a bunch of lines to the process' standard in.
        for (int i = 0; i < 1000000; i++) {
            outputCommand.println("Line " + i);
        }

        // Finish the process
        Result result = null;
        try {
            result = outputCommand.end();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Method end() failed due to IOException");
        }

        // There is a result object
        assertNotNull(result);

        // The exit status of the process is 0
        assertEquals(0, result.exit);

        // There is no error in the standard error log
        assertEquals("", result.stdErr);

        // The first 8K of the standard out are captured
        assertTrue(result.stdOut.endsWith("Line 922\n"));
    }

    @Test
    public void testCommandList() {
        // For multiple commands use the list constructor.
        List<String> cmds = Arrays.asList("/bin/sh", "-c", "_someunknowncommand");
        OutputCommand outputCommand = new OutputCommand(cmds);

        // Start the process
        try {
            outputCommand.start();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Method start() failed due to IOException");
        }

        // Add a bunch of lines to the process' standard in.
        for (int i = 0; i < 10; i++) {
            outputCommand.println("Line " + i);
        }

        // Finish the process
        Result result = null;
        try {
            result = outputCommand.end();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Method end() failed due to IOException");
        }

        // The exit status of the process is 127
        assertEquals(127, result.exit);

        // The error output is captured
        System.out.println(result.stdErr);
        assertTrue(result.stdErr.contains("command not found"));

        // The standard output of the process is empty
        assertEquals("", result.stdOut);
    }

}
