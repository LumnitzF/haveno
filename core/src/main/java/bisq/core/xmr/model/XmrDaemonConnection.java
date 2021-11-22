package bisq.core.xmr.model;

import java.net.URI;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class XmrDaemonConnection {
    @NonNull URI uri;
    String username;
    String password;
    int priority;
    boolean online;
    Boolean authenticated;
}
