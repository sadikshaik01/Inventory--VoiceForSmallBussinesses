package com.voicestock.service;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {
    private final ObjectProvider<DataSource> dataSource;

    public DatabaseHealthService(ObjectProvider<DataSource> dataSource) { this.dataSource = dataSource; }

    public String status() {
        DataSource source = dataSource.getIfAvailable();
        if (source == null) return "NOT_CONFIGURED";
        try (var connection = source.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (SQLException exception) {
            // Do not expose connection strings or database exception details to clients.
            return "DOWN";
        }
    }
}
