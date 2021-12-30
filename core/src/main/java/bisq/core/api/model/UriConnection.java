package bisq.core.api.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class UriConnection {
    @NonNull String uri;
    String username;
    String password;
    int priority;
    OnlineStatus online;
    AuthenticationStatus authenticationStatus;

    public enum OnlineStatus {
        UNKNOWN,
        ONLINE,
        OFFLINE;
    }

    public enum AuthenticationStatus {
        NO_AUTHENTICATION,
        AUTHENTICATED,
        NOT_AUTHENTICATED
    }
}
