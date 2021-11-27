package bisq.core.xmr.daemon.connection.model;

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
    AuthenticationStatus authenticationStatus;

    public enum AuthenticationStatus {
        NO_AUTHENTICATION,
        AUTHENTICATED,
        NOT_AUTHENTICATED
    }
}
