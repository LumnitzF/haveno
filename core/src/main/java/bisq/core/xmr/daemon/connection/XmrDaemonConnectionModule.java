package bisq.core.xmr.daemon.connection;

import bisq.core.xmr.daemon.connection.persistence.XmrConnectionStore;
import bisq.core.xmr.daemon.connection.persistence.model.PersistableXmrConnectionStore;

import bisq.common.app.AppModule;
import bisq.common.config.Config;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XmrDaemonConnectionModule extends AppModule {

    public XmrDaemonConnectionModule(Config config) {
        super(config);
    }

    @Override
    protected final void configure() {
        bind(XmrDaemonConnectionManager.class);
        bind(XmrConnectionStore.class);
        bind(PersistableXmrConnectionStore.class).in(Singleton.class);
    }
}
