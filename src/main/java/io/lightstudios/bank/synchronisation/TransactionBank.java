package io.lightstudios.bank.synchronisation;

import io.lightstudios.bank.LightBank;
import io.lightstudios.bank.api.models.BankData;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Getter
@Setter
public class TransactionBank {

    private int poolSize = 1;
    private long period = 500L; // start value, if not set in the config
    private long delay = 500L; // start value, if not set in the config

    private final ConcurrentLinkedQueue<Transaction> transactionQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(poolSize);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss:SSS");

    public void startTransactions() {
        scheduler.scheduleAtFixedRate(this::processTransactions, delay, period, TimeUnit.MILLISECONDS);
    }

    public void addTransaction(BankData bankData) {
        String timestamp = LocalDateTime.now().format(formatter);
        transactionQueue.add(new Transaction(bankData, timestamp));
    }

    private synchronized void processTransactions() {
        if (transactionQueue.isEmpty()) {
            return;
        }

        Transaction lastTransaction = null;
        // read the last transaction from the queue
        for (Transaction transaction : transactionQueue) {
            lastTransaction = transaction;
        }

        if (lastTransaction != null) {
            UUID uuid = lastTransaction.bankData.getUuid();
            BigDecimal amount = lastTransaction.bankData.getCurrentCoins();
            String timestamp = lastTransaction.timestamp();

            // Write the last transaction to the database asynchronously
            Transaction finalLastTransaction = lastTransaction;

            CompletableFuture.runAsync(() -> {
                LightBank.instance.getBankAccountTable().writeBankData(finalLastTransaction.bankData).thenAccept(result -> {
                    if (result > 0) {
                        if(LightBank.instance.getSettingsConfig().enableDebugMultiSync()) {
                            LightBank.instance.getConsolePrinter().printInfo(
                                    "Processed [" + timestamp + "] bank transaction for " + uuid + ": " + amount);
                        }
                    } else {
                        LightBank.instance.getConsolePrinter().printError(
                                "Failed [" + timestamp + "] bank transaction for " + uuid + ": " + amount);
                    }
                    // Remove the processed transaction from the queue
                    transactionQueue.remove(finalLastTransaction);
                }).exceptionally(throwable -> {
                    LightBank.instance.getConsolePrinter().printError(List.of(
                            "Failed to write last bank transaction for " + uuid,
                            "Amount: " + amount,
                            "Timestamp: " + timestamp));
                    throwable.printStackTrace();
                    transactionQueue.remove(finalLastTransaction);
                    return null;
                });
            }).exceptionally(throwable -> {
                LightBank.instance.getConsolePrinter().printError("Failed to process last bank transaction for " + uuid);
                throwable.printStackTrace();
                transactionQueue.remove(finalLastTransaction);
                return null;
            });

        }
    }

    private record Transaction(BankData bankData, String timestamp) { }

}
