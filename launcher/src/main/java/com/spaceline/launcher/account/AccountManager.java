package com.spaceline.launcher.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the set of launcher accounts: adding, removing, switching the active
 * account and persisting changes. Supports any number of Microsoft and offline
 * accounts simultaneously, mirroring the multi-account UX of modern launchers.
 */
public final class AccountManager {

    private static final Logger LOG = LoggerFactory.getLogger(AccountManager.class);

    private final AccountStore store;
    private final List<Account> accounts = new CopyOnWriteArrayList<>();
    private volatile UUID activeUuid;

    public AccountManager(AccountStore store) {
        this.store = store;
    }

    /** Loads persisted accounts into memory. Call once at startup. */
    public void load() {
        AccountStore.LoadedAccounts loaded = store.load();
        accounts.clear();
        accounts.addAll(loaded.accounts());
        activeUuid = loaded.activeUuid();
        if (activeUuid == null && !accounts.isEmpty()) {
            activeUuid = accounts.get(0).uuid();
        }
        LOG.info("Loaded {} account(s); active = {}", accounts.size(), activeUuid);
    }

    public List<Account> accounts() {
        return List.copyOf(accounts);
    }

    public Optional<Account> active() {
        return find(activeUuid);
    }

    public Optional<Account> find(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return accounts.stream().filter(a -> a.uuid().equals(uuid)).findFirst();
    }

    /** Adds (or replaces by UUID) an account and makes it active. */
    public Account add(Account account) {
        accounts.removeIf(existing -> existing.uuid().equals(account.uuid()));
        accounts.add(account);
        activeUuid = account.uuid();
        persist();
        LOG.info("Added account {} ({})", account.username(), account.type());
        return account;
    }

    /** Convenience for creating and adding an offline account. */
    public Account addOffline(String username) {
        return add(Account.offline(username));
    }

    public void remove(UUID uuid) {
        accounts.removeIf(a -> a.uuid().equals(uuid));
        if (uuid.equals(activeUuid)) {
            activeUuid = accounts.isEmpty() ? null : accounts.get(0).uuid();
        }
        persist();
    }

    /** Switches the active account. Throws if the UUID is unknown. */
    public void setActive(UUID uuid) {
        if (find(uuid).isEmpty()) {
            throw new IllegalArgumentException("No account with UUID " + uuid);
        }
        activeUuid = uuid;
        persist();
        LOG.info("Switched active account to {}", uuid);
    }

    /** Persists token changes made in place (e.g. after a refresh). */
    public void persist() {
        store.save(List.copyOf(accounts), activeUuid);
    }
}
