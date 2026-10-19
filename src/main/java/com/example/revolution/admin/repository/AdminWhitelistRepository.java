package com.example.revolution.admin.repository;

import com.example.revolution.admin.definition.AdminWhitelistEntry;
import java.io.File;
import java.util.List;

public interface AdminWhitelistRepository {
    void initialize(File directory);
    List<AdminWhitelistEntry> load();
    void save(List<AdminWhitelistEntry> entries);
}
