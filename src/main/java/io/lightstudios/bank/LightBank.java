package io.lightstudios.bank;

import io.lightstudios.bank.api.LightBankAPI;
import io.lightstudios.bank.configs.MessageConfig;
import io.lightstudios.bank.configs.SettingsConfig;
import io.lightstudios.bank.storage.BankDataTable;
import io.lightstudios.core.util.ConsolePrinter;
import io.lightstudios.core.util.files.FileManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class LightBank extends JavaPlugin {

    public static LightBank instance;
    private LightBankAPI lightBankAPI;
    private ConsolePrinter consolePrinter;

    private BankDataTable bankAccountTable;

    private MessageConfig messageConfig;
    private SettingsConfig settingsConfig;

    private FileManager messageFile;
    private FileManager settingsFile;

    @Override
    public void onLoad() {
        // Plugin startup logic
        instance = this;
        this.consolePrinter = new ConsolePrinter("§7[§rLight§eBank§7] §r");
        this.consolePrinter.printInfo("Creating LightBank API instance ...");
        this.lightBankAPI = new LightBankAPI();
        this.consolePrinter.printInfo("Creating default configuration files ...");
        readAndWriteFiles();
        this.consolePrinter.printInfo("Selecting plugin language ...");
        selectLanguage();

        this.bankAccountTable = new BankDataTable();

    }

    @Override
    public void onEnable() {
        // Plugin startup logic
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void loadDefaults() {
        readAndWriteFiles();
        selectLanguage();
    }

    private void registerEvents() {

    }

    private void registerCommands() {

    }

    private void unregisterCommands() {

    }

    private void readAndWriteFiles() {

        this.settingsFile = new FileManager(this, "settings.yml", true);
        this.settingsConfig = new SettingsConfig(this.settingsFile);

    }

    private void selectLanguage() {
        String language = settingsConfig.language();

        switch (language) {
            case "de":
                this.messageFile = new FileManager(this, "language/" + "de" + ".yml", true);
                break;
            case "pl":
                this.messageFile = new FileManager(this, "language/" + "pl" + ".yml", true);
                break;
            default:
                this.messageFile = new FileManager(this, "language/" + "en" + ".yml", true);
                break;
        }

        this.messageConfig = new MessageConfig(this.messageFile);

    }
}
