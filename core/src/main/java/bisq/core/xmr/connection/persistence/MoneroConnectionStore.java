package bisq.core.xmr.connection.persistence;

import bisq.core.api.CoreAccountService;
import bisq.core.crypto.ScryptUtil;
import bisq.core.util.Initializable;
import bisq.core.xmr.connection.model.MoneroConnection;
import bisq.core.xmr.connection.persistence.model.PersistableMoneroConnection;
import bisq.core.xmr.connection.persistence.model.PersistableMoneroConnectionStore;

import bisq.common.crypto.CryptoException;
import bisq.common.crypto.Encryption;

import com.google.protobuf.ByteString;

import org.bitcoinj.crypto.KeyCrypterScrypt;

import javax.inject.Inject;
import javax.inject.Singleton;

import javax.crypto.SecretKey;

import java.net.URI;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

@Singleton
public class MoneroConnectionStore implements Initializable {

    private final Object lock = new Object();

    private final PersistableMoneroConnectionStore store;

    private final CoreAccountService accountService;

    private KeyCrypterScrypt keyCrypterScrypt;

    private SecretKey encryptionKey;

    @Inject
    public MoneroConnectionStore(PersistableMoneroConnectionStore store, CoreAccountService accountService) {
        this.store = store;
        this.accountService = accountService;
    }


    @Override
    public void initialize() {
        accountService.addPasswordChangeListener(this::onPasswordChange);
        synchronized (lock) {
            setupStore();
            encryptionKey = toSecretKey(accountService.getPassword());
        }
    }

    private void setupStore() {
        this.keyCrypterScrypt = ScryptUtil.getKeyCrypterScrypt();
        byte[] storedSalt = store.getSalt();
        if (storedSalt != null) {
            this.keyCrypterScrypt = new KeyCrypterScrypt(keyCrypterScrypt.getScryptParameters()
                    .toBuilder()
                    .setSalt(ByteString.copyFrom(storedSalt))
                    .build());
        } else {
            store.setSalt(keyCrypterScrypt.getScryptParameters().getSalt().toByteArray());
        }
    }

    public boolean hasConnection(URI connection) {
        synchronized (lock) {
            return store.hasConnection(connection);
        }
    }

    public List<MoneroConnection> getAllConnections() {
        synchronized (lock) {
            return store.getConnections().stream().map(this::toMoneroConnection).collect(Collectors.toList());
        }
    }

    public void addConnection(MoneroConnection connection) {
        synchronized (lock) {
            PersistableMoneroConnection persistableConnection = toPersistableMoneroConnection(connection);
            store.addConnection(persistableConnection);
        }
        store.requestPersistence();
    }

    public void removeConnection(URI connection) {
        synchronized (lock) {
            store.removeConnection(connection);
        }
        store.requestPersistence();
    }

    private void onPasswordChange(String oldPassword, String newPassword) {
        synchronized (lock) {
            SecretKey oldSecret = encryptionKey;
            assert Objects.equals(oldSecret, toSecretKey(oldPassword)) : "Old secret does not match old password";

            // TODO: Should this be done in some background thread?
            encryptionKey = toSecretKey(newPassword);
            reEncryptStore(store, oldSecret, encryptionKey);
        }
        store.requestPersistence();
    }

    private SecretKey toSecretKey(String password) {
        if (password == null) {
            return null;
        }
        return Encryption.getSecretKeyFromBytes(keyCrypterScrypt.deriveKey(password).getKey());
    }

    private static void reEncryptStore(PersistableMoneroConnectionStore store,
                                       SecretKey oldSecret,
                                       SecretKey newSecret) {
        for (PersistableMoneroConnection connection : store.getConnections()) {
            store.removeConnection(connection.getUri());
            store.addConnection(reEncrypt(connection, oldSecret, newSecret));
        }
    }

    private static PersistableMoneroConnection reEncrypt(PersistableMoneroConnection connection,
                                                         SecretKey oldSecret, SecretKey newSecret) {
        return connection.toBuilder()
                .encryptedPassword(reEncrypt(connection.getEncryptedPassword(), oldSecret, newSecret))
                .build();
    }

    private static byte[] reEncrypt(byte[] value,
                                    SecretKey oldSecret, SecretKey newSecret) {
        if (value == null) {
            // nothing to encrypt
            return null;
        }
        // was previously not encrypted if null
        byte[] decrypted = oldSecret == null ? value : decrypt(value, oldSecret);
        // should not be encrypted if null
        return newSecret == null ? decrypted : encrypt(decrypted, newSecret);
    }

    private static byte[] decrypt(byte[] value, SecretKey secret) {
        try {
            return Encryption.decrypt(value, secret);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Illegal old password", e);
        }
    }

    private static byte[] encrypt(byte[] unencrypted, SecretKey secretKey) {
        try {
            return Encryption.encrypt(unencrypted, secretKey);
        } catch (CryptoException e) {
            throw new RuntimeException("Could not encrypt data with the provided secret", e);
        }
    }

    private PersistableMoneroConnection toPersistableMoneroConnection(MoneroConnection connection) {
        byte[] encryptedPassword = encryptPassword(connection.getPassword());
        return PersistableMoneroConnection.builder()
                .uri(connection.getUri())
                .username(connection.getUsername())
                .priority(connection.getPriority())
                .encryptedPassword(encryptedPassword).build();
    }

    private MoneroConnection toMoneroConnection(PersistableMoneroConnection connection) {
        return MoneroConnection.builder()
                .uri(connection.getUri())
                .username(connection.getUsername())
                .priority(connection.getPriority())
                .password(decryptPassword(connection.getEncryptedPassword()))
                .build();
    }

    @Nullable
    private byte[] encryptPassword(String password) {
        return password != null ? encrypt(password.getBytes(StandardCharsets.UTF_8), encryptionKey) : null;
    }

    @Nullable
    private String decryptPassword(byte[] encypted) {
        return encypted != null ? new String(decrypt(encypted, encryptionKey), StandardCharsets.UTF_8) : null;
    }
}
