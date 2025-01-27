package io.lightstudios.bank.configs;

import io.lightstudios.core.util.files.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

public class SettingsConfig {

    private FileConfiguration config;

    public SettingsConfig(FileManager fileManager) {
        this.config = fileManager.getConfig();
    }

    public String language() { return config.getString("language");}

    public long syncDelay() { return config.getLong("multiTransactionSync.delay");}
    public long syncPeriod() { return config.getLong("multiTransactionSync.period");}
    public boolean enableDebugMultiSync() { return config.getBoolean("multiTransactionSync.enableDebug");}

}
