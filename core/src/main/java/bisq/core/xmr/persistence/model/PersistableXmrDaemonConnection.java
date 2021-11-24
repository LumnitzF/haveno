package bisq.core.xmr.persistence.model;

import bisq.common.proto.persistable.PersistablePayload;

import protobuf.EncryptedMoneroConnection;

import com.google.protobuf.ByteString;

import java.net.URI;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class PersistableXmrDaemonConnection implements PersistablePayload {

    URI uri;
    String username;
    // TODO: current implementation this can be null
    //  This may be a security risk, as a null value can be identified on the disk and used
    //  Can also be "hidden" whether this value is actually set
    byte[] encryptedPassword;
    int priority;

    @Override
    public EncryptedMoneroConnection toProtoMessage() {
        return EncryptedMoneroConnection.newBuilder()
                .setUri(uri.toString())
                .setUsername(username)
                .setEncryptedPassword(ByteString.copyFrom(encryptedPassword))
                .setPriority(priority)
                .build();
    }

    public static PersistableXmrDaemonConnection fromProto(EncryptedMoneroConnection encryptedMoneroConnection) {
        return new PersistableXmrDaemonConnection(
                URI.create(encryptedMoneroConnection.getUri()),
                encryptedMoneroConnection.getUsername(),
                encryptedMoneroConnection.getEncryptedPassword().toByteArray(),
                encryptedMoneroConnection.getPriority());
    }
}
