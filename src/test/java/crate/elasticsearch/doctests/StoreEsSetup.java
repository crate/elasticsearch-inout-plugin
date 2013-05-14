package crate.elasticsearch.doctests;

import com.github.tlrx.elasticsearch.test.EsSetup;

public class StoreEsSetup extends EsSetup {

    public StoreEsSetup() {
        super(new StoreLocalClientProvider());
    }
}
