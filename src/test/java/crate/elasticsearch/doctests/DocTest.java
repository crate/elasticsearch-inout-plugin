package crate.elasticsearch.doctests;

import com.github.tlrx.elasticsearch.test.EsSetup;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import static com.github.tlrx.elasticsearch.test.EsSetup.*;

public class DocTest extends TestCase {

    EsSetup esSetup, esSetup2;

    private static final String PY_TEST = "src/test/python/tests.py";

    private PythonInterpreter interp;
    private PySystemState sys;

    private void resetInterpreter() {
        interp = new PythonInterpreter(null, new PySystemState());
        sys = Py.getSystemState();
    }

    private void execFile(String filePath, String... arguments) {
        interp.cleanup();
        interp.set("__file__", filePath);
        sys.argv = new PyList(new PyString[]{new PyString(filePath)});
        sys.argv.extend(new PyArray(PyString.class, arguments));
        interp.execfile(filePath);
    }

    private void execDocFile(String name) {
        execFile(PY_TEST, name);
    }

    @Before
    public void setUp() {
        if (interp == null) {
            resetInterpreter();
        }
        esSetup = new EsSetup();
        esSetup.execute(deleteAll(), createIndex("users").withSettings(
                fromClassPath("essetup/settings/test_a.json")).withMapping("d",
                fromClassPath("essetup/mappings/test_a.json")).withData(
                fromClassPath("essetup/data/test_a.json")));
        esSetup.client().admin().indices().prepareRefresh("users").execute();
    }

    @After
    public void tearDown() {
        esSetup.terminate();
        if (esSetup2 != null) {
            esSetup2.terminate();
        }
    }

    public void testSearchInto() throws Exception {
        execDocFile("search_into.txt");
    }

}
