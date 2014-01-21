package crate.elasticsearch.doctests;

import crate.elasticsearch.plugin.inout.InOutPlugin;
import junit.framework.TestCase;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import java.io.IOException;

import static org.elasticsearch.common.io.Streams.copyToBytesFromClasspath;
import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE, numNodes = 2)
public class DocTest extends ElasticsearchIntegrationTest {

    private static final String PY_TEST = "src/test/python/tests.py";

    private PythonInterpreter interp;
    private PySystemState sys;

    static {
        System.setProperty("python.cachedir.skip", "true");
        System.setProperty("python.console.encoding", "UTF-8");
    }

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

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings settings = ImmutableSettings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("plugin.types", InOutPlugin.class.getName())
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put("gateway.type", "local")
                .put("http.port", 9200 + nodeOrdinal)
                .put("force.http.enabled", true)
                .build();
        return settings;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (interp == null) {
            resetInterpreter();
        }

        cluster().ensureAtLeastNumNodes(2);
        cluster().ensureAtMostNumNodes(2);
        prepareCreate("users", 1, settingsBuilder().put("index.number_of_shards", 2).put("index.number_of_replicas", 0))
                .addMapping("d", jsonBuilder().startObject()
                        .startObject("d")
                        .startObject("properties")
                        .startObject("name")
                        .field("type", "string")
                        .field("index", "not_analyzed")
                        .field("store", "yes")
                        .endObject()
                        .endObject()
                        .endObject())
                .execute().actionGet();
        ensureGreen("users");

        index("users", "d", "1", "name", "car");
        index("users", "d", "2", "name", "bike");
        index("users", "d", "3", "name", "train");
        index("users", "d", "4", "name", "bus");

        refresh();
        waitForRelocation();
    }

    @Test
    @Ignore("need a second cluster for this test to run")
    public void testSearchInto() throws Exception {
        execDocFile("search_into.rst");
    }

    @Test
    public void testReindex() throws Exception {
        execDocFile("reindex.rst");
    }

}
