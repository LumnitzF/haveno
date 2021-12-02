package bisq.core.xmr.connection.persistence;

import bisq.core.api.CoreAccountService;
import bisq.core.crypto.ScryptUtil;
import bisq.core.util.Initializable;
import bisq.core.xmr.connection.model.UriConnection;
import bisq.core.xmr.connection.persistence.model.PersistableMoneroConnection;
import bisq.core.xmr.connection.persistence.model.PersistableMoneroConnectionStore;

import bisq.common.crypto.CryptoException;
import bisq.common.crypto.Encryption;

import com.google.protobuf.ByteString;

import org.bitcoinj.crypto.KeyCrypterScrypt;

import javax.inject.Inject;
import javax.inject.Singleton;

import javax.crypto.SecretKey;

import java.security.SecureRandom;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

/**
 * Store for {@link UriConnection}s.
 * <p>
 * Passwords are encrypted when stored onto disk, using the account password.
 * If a connection has no password, this is "hidden" by using some random value as fake password.
 *
 * @implNote The password encryption mechanism is handled as follows.
 *  A random salt is generated and stored for each connection. If the connection has no password,
 *  the salt is used as prefix and some random data is attached as fake password. If the connection has a password,
 *  the salt is used as suffix to the actual password. When the password gets decrypted, it is checked whether the
 *  salt is a prefix of the decrypted value. If it is a prefix, the connection has no password.
 *  Otherwise, it is removed (from the end) and the remaining value is the actual password.
 */
@Singleton
public class MoneroConnectionStore implements Initializable {

    private static final int MIN_FAKE_PASSWORD_LENGTH = 5;
    private static final int MAX_FAKE_PASSWORD_LENGTH = 32;
    private static final int SALT_LENGTH = 16;

    private final Object lock = new Object();

    private final SecureRandom random = new SecureRandom();

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
        synchronized (lock) {
            setupStore();
            encryptionKey = toSecretKey(accountService.getPassword());
            accountService.addPasswordChangeListener(this::onPasswordChange);
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

    public boolean hasConnection(String connection) {
        synchronized (lock) {
            return store.hasConnection(connection);
        }
    }

    public List<UriConnection> getAllConnections() {
        synchronized (lock) {
            return store.getConnections().stream().map(this::toUriConnection).collect(Collectors.toList());
        }
    }

    public void addConnection(UriConnection connection) {
        synchronized (lock) {
            PersistableMoneroConnection persistableConnection = toPersistableMoneroConnection(connection);
            store.addConnection(persistableConnection);
        }
        store.requestPersistence();
    }

    public void removeConnection(String connection) {
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
        // Lock the store, so that it isn't persisted in some undefined state
        Lock writeLock = store.getWriteLock();
        writeLock.lock();
        try {
            for (PersistableMoneroConnection connection : store.getConnections()) {
                store.removeConnection(connection.getUri());
                store.addConnection(reEncrypt(connection, oldSecret, newSecret));
            }
        } finally {
            writeLock.unlock();
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
        // was previously not encrypted if null
        byte[] decrypted = oldSecret == null ? value : decrypt(value, oldSecret);
        // should not be encrypted if null
        return newSecret == null ? decrypted : encrypt(decrypted, newSecret);
    }

    private static byte[] decrypt(byte[] encrypted, SecretKey secret) {
        try {
            return Encryption.decrypt(encrypted, secret);
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

    private PersistableMoneroConnection toPersistableMoneroConnection(UriConnection connection) {
        String password = connection.getPassword();
        byte[] passwordBytes = password == null ? null : password.getBytes(StandardCharsets.UTF_8);
        byte[] passwordSalt = generateSalt(passwordBytes);
        byte[] encryptedPassword = encryptPassword(passwordBytes, passwordSalt);

        return PersistableMoneroConnection.builder()
                .uri(connection.getUri())
                .username(connection.getUsername())
                .priority(connection.getPriority())
                .encryptedPassword(encryptedPassword)
                .encryptionSalt(passwordSalt)
                .build();
    }

    private UriConnection toUriConnection(PersistableMoneroConnection connection) {
        byte[] decryptedPasswordBytes = decryptPassword(connection.getEncryptedPassword(), connection.getEncryptionSalt());
        String password = decryptedPasswordBytes == null ? null : new String(decryptedPasswordBytes, StandardCharsets.UTF_8);
        return UriConnection.builder()
                .uri(connection.getUri())
                .username(connection.getUsername())
                .priority(connection.getPriority())
                .password(password)
                .build();
    }


    private byte[] encryptPassword(byte[] password, byte[] salt) {
        byte[] saltedPassword;
        if (password == null) {
            // no password given, so use salt as prefix and add some random data, which disguises itself as password
            int fakePasswordLength = random.nextInt(MAX_FAKE_PASSWORD_LENGTH - MIN_FAKE_PASSWORD_LENGTH + 1)
                    + MIN_FAKE_PASSWORD_LENGTH;
            byte[] fakePassword = new byte[fakePasswordLength];
            random.nextBytes(fakePassword);
            saltedPassword = new byte[salt.length + fakePasswordLength];
            System.arraycopy(salt, 0, saltedPassword, 0, salt.length);
            System.arraycopy(fakePassword, 0, saltedPassword, salt.length, fakePassword.length);
        } else {
            // password given, so append salt to end
            saltedPassword = new byte[password.length + salt.length];
            System.arraycopy(password, 0, saltedPassword, 0, password.length);
            System.arraycopy(salt, 0, saltedPassword, password.length, salt.length);
        }
        return encrypt(saltedPassword, encryptionKey);
    }

    @Nullable
    private byte[] decryptPassword(byte[] encryptedSaltedPassword, byte[] salt) {
        byte[] decryptedSaltedPassword = decrypt(encryptedSaltedPassword, encryptionKey);
        if (arrayStartsWith(decryptedSaltedPassword, salt)) {
            // salt is prefix, so no actual password set
            return null;
        } else {
            // remove salt suffix, the rest is the actual password
            byte[] decryptedPassword = new byte[decryptedSaltedPassword.length - salt.length];
            System.arraycopy(decryptedSaltedPassword, 0, decryptedPassword, 0, decryptedPassword.length);
            return decryptedPassword;
        }
    }

    private byte[] generateSalt(byte[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        // Generate salt, that is guaranteed to be no prefix of the password
        do {
            random.nextBytes(salt);
        } while (arrayStartsWith(password, salt));
        return salt;
    }

    private static boolean arrayStartsWith(byte[] container, byte[] prefix) {
        if (container.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (container[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
