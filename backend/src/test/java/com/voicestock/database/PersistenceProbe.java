package com.voicestock.database;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import com.voicestock.VoiceStockApplication;
import com.voicestock.entity.*;
import com.voicestock.repository.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Test-only entry point: never packaged into the application JAR or exposed over HTTP. */
public class PersistenceProbe {
    public static void main(String[] args) throws Exception {
        String action = args[0];
        Path manifest = Path.of(args[1]);
        try (ConfigurableApplicationContext context = SpringApplication.run(VoiceStockApplication.class, TestDatabase.applicationArguments())) {
            var unitOfWork = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            var users = context.getBean(UserRepository.class);
            var products = context.getBean(ProductRepository.class);
            var transactions = context.getBean(InventoryTransactionRepository.class);
            var actions = context.getBean(BusinessActionRepository.class);
            var ids = new Properties();
            if (action.equals("write")) {
                unitOfWork.executeWithoutResult(status -> {
                    var user = users.saveAndFlush(TestDatabase.user());
                    var product = products.saveAndFlush(new Product(user, "Restart persistence test", "Verification",
                            InventoryUnit.BAGS, new BigDecimal("7.250"), new BigDecimal("2.000"), null));
                    var entry = transactions.saveAndFlush(new InventoryTransaction(user, product, TransactionType.ADD,
                            new BigDecimal("7.250"), InventoryUnit.BAGS, TransactionSource.MANUAL));
                    var businessAction = actions.saveAndFlush(new BusinessAction(product,new BigDecimal("3"),"Test-only restart verification"));
                    ids.setProperty("action",businessAction.getId().toString());
                    ids.setProperty("user", user.getId().toString());
                    ids.setProperty("product", product.getId().toString());
                    ids.setProperty("transaction", entry.getId().toString());
                });
                try (var output = Files.newOutputStream(manifest)) { ids.store(output, "Temporary test record IDs; no credentials"); }
                System.out.println("PERSISTENCE_WRITE_COMMITTED");
            } else {
                try (var input = Files.newInputStream(manifest)) { ids.load(input); }
                UUID userId = UUID.fromString(ids.getProperty("user"));
                UUID productId = UUID.fromString(ids.getProperty("product"));
                UUID actionId = UUID.fromString(ids.getProperty("action"));
                UUID transactionId = UUID.fromString(ids.getProperty("transaction"));
                unitOfWork.executeWithoutResult(status -> {
                    if (action.equals("read")) {
                        if (!users.existsById(userId)) throw new AssertionError("User did not survive restart");
                        var product = products.findByIdAndUserId(productId, userId).orElseThrow();
                        var entry = transactions.findByIdAndUserId(transactionId, userId).orElseThrow();
                        if (product.getCurrentStock().compareTo(new BigDecimal("7.250")) != 0
                                || entry.getQuantity().compareTo(new BigDecimal("7.250")) != 0
                                || entry.getSource() != TransactionSource.MANUAL
                                || entry.getUnit() != InventoryUnit.BAGS) throw new AssertionError("Stored values changed");
                        var savedAction=actions.findById(actionId).orElseThrow();
                        if(!savedAction.getStatus().equals("APPROVED") || savedAction.getApprovedAt()==null) throw new AssertionError("Approved action did not survive restart");
                        System.out.println("PERSISTENCE_RESTART_VERIFIED");
                    } else if (action.equals("complete")) {
                        var saved=actions.findById(actionId).orElseThrow();saved.complete();actions.saveAndFlush(saved);
                        System.out.println("ACTION_COMPLETED");
                    } else if (action.equals("readCompleted")) {
                        var saved=actions.findById(actionId).orElseThrow();
                        if(!saved.getStatus().equals("COMPLETED") || saved.getCompletedAt()==null || saved.getProduct().getCurrentStock().compareTo(new BigDecimal("7.250"))!=0) throw new AssertionError("Completed action or stock changed after restart");
                        System.out.println("COMPLETED_ACTION_RESTART_VERIFIED");
                    } else if (action.equals("cleanup")) {
                        // Delete only this probe's UUIDs, in FK order. Never truncate or drop tables.
                        var jdbc = context.getBean(JdbcTemplate.class);
                        jdbc.update("DELETE FROM business_actions WHERE id=? AND user_id=?",actionId,userId);
                        jdbc.update("DELETE FROM inventory_transactions WHERE id=? AND user_id=?", transactionId, userId);
                        jdbc.update("DELETE FROM products WHERE id=? AND user_id=?", productId, userId);
                        jdbc.update("DELETE FROM users WHERE id=?", userId);
                    } else { throw new IllegalArgumentException("Unknown probe action"); }
                });
            }
        }
    }
}

