package io.lightstudios.bank.storage;

import io.lightstudios.bank.LightBank;
import io.lightstudios.bank.api.models.BankAccount;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.database.model.DatabaseTypes;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BankAccountTable {

    private final String tableName = "lightbank_bank";

    public BankAccountTable() {
        LightBank.instance.getConsolePrinter().printInfo("Initializing BankAccountTable and creating Table...");
        createTable();
    }

    public CompletableFuture<List<BankAccount>> readBankAccount() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT uuid, name, coins, level FROM " + tableName;
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query);
                     ResultSet resultSet = statement.executeQuery()) {

                    List<BankAccount> bankAccountList = new ArrayList<>();
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        String name = resultSet.getString("name");
                        BigDecimal coins = resultSet.getBigDecimal("coins");
                        int level = resultSet.getInt("level");

                        BankAccount bankAccount = new BankAccount(uuid);
                        bankAccount.setCurrentCoins(coins);
                        bankAccount.setName(name);

                        bankAccountList.add(bankAccount);
                    }
                    return bankAccountList;
                } catch (Exception e) {
                    LightBank.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while reading bank data from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }).exceptionally(e -> {
            LightBank.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading bank data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException(e);
        });
    }

    public CompletableFuture<BankAccount> findBankAccountByUUID(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "SELECT uuid, name, coins, level FROM " + tableName + " WHERE uuid = ?";
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, id.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                            String name = resultSet.getString("name");
                            BigDecimal coins = resultSet.getBigDecimal("coins");
                            int level = resultSet.getInt("level");

                            BankAccount bankAccount = new BankAccount(uuid);
                            bankAccount.setCurrentCoins(coins);
                            bankAccount.setName(name);
                            return bankAccount;
                        } else {
                            return null; // No player found with the given UUID
                        }
                    }
                } catch (Exception e) {
                    LightBank.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while reading player data from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    throw new RuntimeException("An error occurred while reading player data from the database!", e);
                }
            }
        }).exceptionally(e -> {
            LightBank.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException(e);
        });
    }

    public CompletableFuture<Integer> writeBankAccount(BankAccount bankAccount) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query;
                if (LightCore.instance.getSqlDatabase().getDatabaseType().equals(DatabaseTypes.SQLITE)) {
                    query = "INSERT OR REPLACE INTO " + tableName + " (uuid, name, coins, level) VALUES (?, ?, ?, ?)";
                } else {
                    query = "INSERT INTO " + tableName + " (uuid, name, coins, level) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE coins = VALUES(coins)";
                }
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, bankAccount.getUuid().toString());
                    statement.setString(2, bankAccount.getName());
                    statement.setBigDecimal(3, bankAccount.getCurrentCoins());
                    statement.setInt(4, 1); // TODO: Implement level system
                    return statement.executeUpdate();
                } catch (Exception e) {
                    LightBank.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while creating a new player in the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    throw new RuntimeException("An error occurred while creating a new player in the database!", e);
                }
            }
        }).thenApply(result -> {
            if (result < 1) {
                LightBank.instance.getConsolePrinter().printError(List.of(
                        "No rows were inserted in the database!",
                        "Please check the error logs for more information."
                ));
                throw new RuntimeException("No rows were inserted in the database!");
            }
            return result;
        }).exceptionally(e -> {
            LightBank.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while creating a new player in the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException(e);
        });
    }

    public CompletableFuture<Boolean> deleteBankAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                String query = "DELETE FROM " + tableName + " WHERE uuid = ?";
                try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, uuid.toString());
                    int result = statement.executeUpdate();
                    if (result < 1) {
                        LightBank.instance.getConsolePrinter().printError(List.of(
                                "An error occurred while deleting account from the database!",
                                "Please check the error logs for more information."
                        ));
                        return false;
                    }
                    return true;
                } catch (SQLException e) {
                    LightBank.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while deleting account from the database!",
                            "Please check the error logs for more information."
                    ));
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }


    public void createTable() {
        synchronized (this) {
            String query = createCoinsTable();
            LightBank.instance.getConsolePrinter().printInfo("Creating bank table...");
            try (Connection connection = LightCore.instance.getSqlDatabase().getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
                LightBank.instance.getConsolePrinter().printInfo("Coins table created successfully!");
            } catch (SQLException e) {
                LightBank.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while creating the bank table!",
                        "Please check the error logs for more information.",
                        "Query: " + query
                ));
                e.printStackTrace();
            }
        }
    }

    private @NotNull String createCoinsTable() {

        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid VARCHAR(36) NOT NULL UNIQUE, "
                + "name VARCHAR(36), "
                + "coins DECIMAL(65, 2), "
                + "level INT, "
                + "PRIMARY KEY (uuid))";
    }

}
