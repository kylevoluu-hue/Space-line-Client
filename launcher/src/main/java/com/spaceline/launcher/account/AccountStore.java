package com.spaceline.launcher.account;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.spaceline.common.config.ConfigManager;
import com.spaceline.common.config.Versioned;

/**
 * Persists the set of accounts (and which one is active) to a versioned JSON
 * file under the accounts data directory.
 *
 * <p>Access/refresh tokens are stored so the user stays signed in between runs.
 * They live under the per-user data directory with the same protection as the
 * rest of the profile; treat that directory as sensitive.
 */
public final class AccountStore {

    /** The serialized shape of one account. */
    static final class Entry {
        String type;
        String uuid;
        String username;
        String accessToken;
        String refreshToken;
        String expiry;
    }

    /** The versioned document: all accounts plus the active selection. */
    static final class Document implements Versioned {
        int currentVersion = 1;
        List<Entry> accounts = new ArrayList<>();
        String activeUuid;

        @Override
        public int currentVersion() {
            return currentVersion;
        }
    }

    private final ConfigManager<Document> config;

    public AccountStore(Path accountsDir) {
        this.config = new ConfigManager<>(accountsDir.resolve("accounts.json"),
                Document.class, Document::new);
    }

    public LoadedAccounts load() {
        Document document = config.load();
        List<Account> accounts = new ArrayList<>();
        for (Entry entry : document.accounts) {
            accounts.add(toAccount(entry));
        }
        UUID active = document.activeUuid == null ? null : UUID.fromString(document.activeUuid);
        return new LoadedAccounts(accounts, active);
    }

    public void save(List<Account> accounts, UUID activeUuid) {
        Document document = new Document();
        for (Account account : accounts) {
            document.accounts.add(toEntry(account));
        }
        document.activeUuid = activeUuid == null ? null : activeUuid.toString();
        config.save(document);
    }

    public record LoadedAccounts(List<Account> accounts, UUID activeUuid) {
    }

    private static Entry toEntry(Account account) {
        Entry entry = new Entry();
        entry.type = account.type().name();
        entry.uuid = account.uuid().toString();
        entry.username = account.username();
        if (account.type() == AuthType.MICROSOFT) {
            entry.accessToken = account.accessToken();
            entry.refreshToken = account.refreshToken();
            entry.expiry = account.accessTokenExpiry() == null
                    ? null : account.accessTokenExpiry().toString();
        }
        return entry;
    }

    private static Account toAccount(Entry entry) {
        AuthType type = AuthType.valueOf(entry.type);
        if (type == AuthType.OFFLINE) {
            return Account.offline(entry.username);
        }
        Instant expiry = entry.expiry == null ? null : Instant.parse(entry.expiry);
        return Account.microsoft(UUID.fromString(entry.uuid), entry.username,
                entry.accessToken, entry.refreshToken, expiry);
    }
}
