package com.firstblick.elasticsearch.module.export.test;

import com.firstblick.elasticsearch.action.export.ExportAction;
import com.firstblick.elasticsearch.action.export.ExportRequest;
import com.firstblick.elasticsearch.action.export.ExportResponse;
import com.firstblick.elasticsearch.rest.action.admin.export.RestExportAction;
import com.github.tlrx.elasticsearch.test.EsSetup;
import junit.framework.TestCase;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestXContentBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.io.IOException;

import static com.github.tlrx.elasticsearch.test.EsSetup.*;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

public class RestExportActionTest extends TestCase {

    EsSetup esSetup;

    RestExportAction restExportAction;


    @Before
    public void setUp() {
        esSetup = new EsSetup();
        //esSetup.execute(
        //        deleteAll(),
        //       createIndex("users")
        //);
    }

    @After
    public void tearDown() {
        esSetup.terminate();
    }

    @Test
    public void testPlainCall() {
//        ExportRequest exportRequest = new ExportRequest();
//        esSetup.client().execute(ExportAction.INSTANCE, exportRequest, new ActionListener<ExportResponse>() {
//
//            @Override
//            public void onResponse(ExportResponse response) {
//            }
//
//            @Override
//            public void onFailure(Throwable e) {
//
//            }
//        });

    }
}
