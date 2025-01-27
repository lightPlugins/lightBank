package io.lightstudios.bank.api.models;

import io.lightstudios.bank.LightBank;
import io.lightstudios.bank.api.BankResponse;
import io.lightstudios.bank.synchronisation.TransactionBank;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.libs.jedis.Jedis;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BankData {

    private final UUID uuid;
    private String name;
    private BankLevel bankLevel;
    private BigDecimal currentCoins;
    private String currencySingular;
    private String currencyPlural;

    private static final String REDIS_CHANNEL = "bankAccountUpdates";
    private static final TransactionBank transactionManager = new TransactionBank();


    public BankData(UUID uuid) {
        this.uuid = uuid;
        this.name = "unknown";
        this.currentCoins = new BigDecimal(0);
        this.currencySingular = "Coin";
        this.currencyPlural = "Coins";

        transactionManager.setDelay(500);
        transactionManager.setPeriod(500);
        transactionManager.startTransactions();
    }

    public boolean hasEnough(BigDecimal amount) {

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            BankData result = LightBank.instance.getBankAccountTable().findBankDataByUUID(uuid).join();
            return result.getCurrentCoins().compareTo(amount) >= 0;
        } else {
            return currentCoins.compareTo(amount) >= 0;
        }
    }

    /**
     * Adds coins to the players personal bank.
     * @param coins The amount of coins to add.
     * @return The response of the transaction.
     */
    public BankResponse addCoins(BigDecimal coins) {
        BankResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return defaultResponse;
        }

        if(this.currentCoins.add(coins).compareTo(bankLevel.getMaxBalance()) > 0) {
            return new BankResponse(coins, currentCoins,
                    BankResponse.BankResponseType.MAX_BALANCE_EXCEED, "Max Bank balance exceeded by level.");
        }

        this.currentCoins = this.currentCoins.add(coins);

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            // TODO: Update the data in the database directly
            LightBank.instance.getBankAccountTable().writeBankData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionManager.addTransaction(this);
        }

        return new BankResponse(coins, currentCoins, BankResponse.BankResponseType.SUCCESS, "");
    }

    /**
     * Remove coins from the players personal bank.
     * @param coins The amount of coins to add.
     * @return The response of the transaction.
     */
    public BankResponse removeCoins(BigDecimal coins) {

        BankResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return defaultResponse;
        }

        if(!hasEnough(coins)) {
            return new BankResponse(coins, this.currentCoins,
                    BankResponse.BankResponseType.FAILURE, "Not enough coins.");
        }

        this.currentCoins = this.currentCoins.subtract(coins);
        // Update the data in the database directly or through the transaction manager (redis)
        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            LightBank.instance.getBankAccountTable().writeBankData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionManager.addTransaction(this);
        }

        return new BankResponse(coins, currentCoins, BankResponse.BankResponseType.SUCCESS, "");
    }

    /**
     * Sets the coins for the players personal bank.
     * @param coins The amount of coins to set.
     * @return The response of the transaction.
     */
    public BankResponse setCoins(BigDecimal coins) {
        BankResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return new BankResponse(coins, this.currentCoins,
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        this.currentCoins = coins;
        // Update the data in the database directly or through the transaction manager (redis)
        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            LightBank.instance.getBankAccountTable().writeBankData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionManager.addTransaction(this);
        }

        return new BankResponse(coins, this.currentCoins,
                BankResponse.BankResponseType.SUCCESS, "");
    }

    /**
     * Get the formatted currency (plural/singular) for messages.
     * @return The response of the transaction.
     */
    public String getFormattedCurrency() {
        return currentCoins.compareTo(BigDecimal.ONE) == 0 ? currencySingular : currencyPlural;
    }

    /**
     * Get the formatted coins for messages.
     * @return The response of the transaction.
     */
    public String getFormattedCoins() {
        return LightNumbers.formatForMessages(currentCoins, 2);
    }


    private BankResponse checkDefaults(BigDecimal coinsToAdd) {
        if (coinsToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            return new BankResponse(coinsToAdd, currentCoins,
                    BankResponse.BankResponseType.NOT_NEGATIVE, "Cannot add negative or zero coins.");
        }
        return new BankResponse(BigDecimal.valueOf(0), BigDecimal.valueOf(0),
                BankResponse.BankResponseType.SUCCESS, "");
    }

    /**
     * Sends the current coins data to the Redis server and
     * synchronizes the data with the other servers.
     */
    private void sendUpdateToRedis() {
        try (Jedis jedis = LightCore.instance.getRedisManager().getJedisPool().getResource()) {
            if (uuid == null || currentCoins == null) {
                LightBank.instance.getConsolePrinter().printError(List.of(
                        "UUID, amount, or currentBalance cannot be null in BankAccount!",
                        "UUID: " + uuid,
                        "Current Coins: " + currentCoins,
                        "Could not send update to Redis. This behavior is unexpected",
                        "and you should report this to the plugin developer!"
                ));
                throw new IllegalArgumentException("UUID, amount, or currentCoins cannot be null");
            }
            String message = String.format("%s:%s:%s:%s", this.uuid, this.name, this.currentCoins, this.bankLevel);
            jedis.publish(REDIS_CHANNEL, message);
        } catch (Exception e) {
            // Log the exception or handle it accordingly
            e.printStackTrace();
        }
    }


}
