package bisq.core.xmr.connection;

import bisq.core.xmr.connection.persistence.MoneroConnectionStore;
import bisq.core.xmr.connection.persistence.model.XmrConnectionList;

import bisq.common.app.AppModule;
import bisq.common.config.Config;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoneroConnectionModule extends AppModule {

    public MoneroConnectionModule(Config config) {
        super(config);
    }

    @Override
    protected final void configure() {
        bind(MoneroConnectionsManager.class);
        bind(MoneroConnectionStore.class);
        bind(XmrConnectionList.class).in(Singleton.class);
    }
}
