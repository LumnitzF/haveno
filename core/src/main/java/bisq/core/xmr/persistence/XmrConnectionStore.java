package bisq.core.xmr.persistence;

import bisq.core.api.CoreAccountService;
import bisq.core.util.Initializable;
import bisq.core.xmr.model.XmrDaemonConnection;
import bisq.core.xmr.persistence.model.PersistableXmrConnectionStore;
import bisq.core.xmr.persistence.model.PersistableXmrDaemonConnection;

import bisq.common.crypto.CryptoException;
import bisq.common.crypto.Encryption;

import javax.inject.Inject;
import javax.inject.Singleton;

import javax.crypto.SecretKey;

import java.net.URI;

import java.nio.charset.StandardCharsets;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

@Singleton
public class XmrConnectionStore implements Initializable {

    private final Object lock = new Object();

    private final PersistableXmrConnectionStore store;

    private final CoreAccountService accountService;

    private SecretKey encryptionKey;

    @Inject
    public XmrConnectionStore(PersistableXmrConnectionStore store, CoreAccountService accountService) {
        this.store = store;
        this.accountService = accountService;
    }


    @Override
    public void initialize() {
        accountService.addPasswordChangeListener(this::onPasswordChange);
        synchronized (lock) {
            encryptionKey = toSecretKey(accountService.getPassword());
        }
    }

    public void addConnection(XmrDaemonConnection connection) {
        synchronized (lock) {
            PersistableXmrDaemonConnection persistableConnection = toPersistableConnection(connection);
            store.addConnection(persistableConnection);
        }
    }

    public void removeConnection(URI connection) {
        synchronized (lock) {
            store.removeConnection(connection);
        }
    }

    private void onPasswordChange(String oldPassword, String newPassword) {
        synchronized (lock) {
            SecretKey oldSecret = encryptionKey;
            assert Objects.equals(oldSecret, toSecretKey(oldPassword)) : "Old secret does not match old password";

            encryptionKey = toSecretKey(newPassword);
            reEncryptStore(store, oldSecret, encryptionKey);
        }
    }

    private SecretKey toSecretKey(String password) {
        if (password == null) {
            return null;
        }
        return Encryption.getSecretKeyFromBytes(password.getBytes(StandardCharsets.UTF_8));
    }

    private static void reEncryptStore(PersistableXmrConnectionStore store,
                                       SecretKey oldSecret,
                                       SecretKey newSecret) {
        for (PersistableXmrDaemonConnection connection : store.getConnections()) {
            store.removeConnection(connection.getUri());
            store.addConnection(reEncrypt(connection, oldSecret, newSecret));
        }
    }

    private static PersistableXmrDaemonConnection reEncrypt(PersistableXmrDaemonConnection connection,
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

    private PersistableXmrDaemonConnection toPersistableConnection(XmrDaemonConnection connection) {
        byte[] encryptedPassword = encryptPassword(connection.getPassword());
        return PersistableXmrDaemonConnection.builder()
                .uri(connection.getUri())
                .username(connection.getUsername())
                .priority(connection.getPriority())
                .encryptedPassword(encryptedPassword).build();
    }

    @Nullable
    private byte[] encryptPassword(String password) {
        return password != null ? encrypt(password.getBytes(StandardCharsets.UTF_8), encryptionKey) : null;
    }
}
