package io.lightstudios.bank.configs;

import io.lightstudios.core.util.files.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

public class SettingsConfig {

    private FileConfiguration config;

    public SettingsConfig(FileManager fileManager) {
        this.config = fileManager.getConfig();
    }

    public String language() { return config.getString("language");}

}
