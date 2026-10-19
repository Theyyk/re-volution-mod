package com.example.revolution.admin.definition;

/** JSON whitelist entry; its persisted field names remain uuid and name. */
public final class AdminWhitelistEntry {
    public final String uuid;
    public final String name;
    public AdminWhitelistEntry(String uuid, String name) { this.uuid = uuid; this.name = name; }
}
