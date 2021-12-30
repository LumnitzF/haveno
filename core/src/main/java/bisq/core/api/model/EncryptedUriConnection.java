package bisq.core.api.model;

import bisq.common.proto.persistable.PersistablePayload;

import com.google.protobuf.ByteString;

import lombok.Builder;
import lombok.Value;


@Value
@Builder(toBuilder = true)
public class EncryptedUriConnection implements PersistablePayload {

    String uri;
    String username;
    byte[] encryptedPassword;
    byte[] encryptionSalt;
    int priority;

    @Override
    public protobuf.EncryptedUriConnection toProtoMessage() {
        return protobuf.EncryptedUriConnection.newBuilder()
                .setUri(uri)
                .setUsername(username)
                .setEncryptedPassword(ByteString.copyFrom(encryptedPassword))
                .setEncryptionSalt(ByteString.copyFrom(encryptionSalt))
                .setPriority(priority)
                .build();
    }

    public static EncryptedUriConnection fromProto(protobuf.EncryptedUriConnection encryptedMoneroConnection) {
        return new EncryptedUriConnection(
                encryptedMoneroConnection.getUri(),
                encryptedMoneroConnection.getUsername(),
                encryptedMoneroConnection.getEncryptedPassword().toByteArray(),
                encryptedMoneroConnection.getEncryptionSalt().toByteArray(),
                encryptedMoneroConnection.getPriority());
    }
}
