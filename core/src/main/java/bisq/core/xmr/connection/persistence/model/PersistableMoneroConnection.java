package bisq.core.xmr.connection.persistence.model;

import bisq.common.proto.persistable.PersistablePayload;

import protobuf.EncryptedMoneroConnection;

import com.google.protobuf.ByteString;

import java.net.URI;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class PersistableMoneroConnection implements PersistablePayload {

    URI uri;
    String username;
    byte[] encryptedPassword;
    byte[] encryptionSalt;
    int priority;

    @Override
    public EncryptedMoneroConnection toProtoMessage() {
        return EncryptedMoneroConnection.newBuilder()
                .setUri(uri.toString())
                .setUsername(username)
                .setEncryptedPassword(ByteString.copyFrom(encryptedPassword))
                .setEncryptionSalt(ByteString.copyFrom(encryptionSalt))
                .setPriority(priority)
                .build();
    }

    public static PersistableMoneroConnection fromProto(EncryptedMoneroConnection encryptedMoneroConnection) {
        return new PersistableMoneroConnection(
                URI.create(encryptedMoneroConnection.getUri()),
                encryptedMoneroConnection.getUsername(),
                encryptedMoneroConnection.getEncryptedPassword().toByteArray(),
                encryptedMoneroConnection.getEncryptionSalt().toByteArray(),
                encryptedMoneroConnection.getPriority());
    }
}
