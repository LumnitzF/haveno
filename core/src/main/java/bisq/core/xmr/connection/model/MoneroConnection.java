package bisq.core.xmr.connection.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class MoneroConnection {
    @NonNull String uri;
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
