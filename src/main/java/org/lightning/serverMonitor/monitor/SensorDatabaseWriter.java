package org.lightning.serverMonitor.monitor;

import org.lightning.serverMonitor.sensorMonitoring.SensorDump;
import org.lightning.serverMonitor.sensorMonitoring.SensorDevice;
import org.lightning.serverMonitor.sensorMonitoring.SensorProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import java.sql.*;
import java.util.*;

public class SensorDatabaseWriter {
    private static final String URL = "jdbc:sqlite:sensors.db";

    public static void saveMultipleDumps(List<SensorDump> dumpList) {
        if (dumpList.isEmpty()) return;
        createNewTable();
        // 1. Discover all unique sensor keys from the first dump to define the schema
        Set<String> discoveredKeys = new HashSet<>();
        for (SensorDevice device : dumpList.get(0).devices) {
            for (SensorProperty prop : device.properties) {
                discoveredKeys.add(getSensorColumn(device, prop));
            }
        }

        // 2. Ensure table has columns for all discovered keys
        ensureColumnsExist(discoveredKeys);

        // 3. Insert the data (Flattening into one row per t)
        String sql = buildInsertSql(discoveredKeys);
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (SensorDump dump : dumpList) {
                pstmt.setLong(1, dump.timestamp);

                // Map of all keys in this specific dump
                Map<String, String> rowData = new HashMap<>();
                for (SensorDevice d : dump.devices)
                    for (SensorProperty p : d.properties) rowData.put(getSensorColumn(d, p), p.value);

                // Set values dynamically for each column
                int colIndex = 2;
                for (String key : discoveredKeys) {
                    pstmt.setString(colIndex++, rowData.getOrDefault(key, null));
                }
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save sensor logs", e);
        }
    }

    public static void createNewTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS sensor_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL
                );
                """;

        try (Connection conn = DriverManager.getConnection(URL);
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create base sensor_logs table", e);
        }
    }

    private static void ensureColumnsExist(Set<String> keys) {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "sensor_logs", null);
            Set<String> existingColumns = new HashSet<>();
            while (rs.next()) existingColumns.add(rs.getString("COLUMN_NAME"));

            for (String key : keys) {
                if (!existingColumns.contains(key)) {
                    // Sanitize key for SQL (remove special chars)
                    String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "_");
                    stmt.execute("ALTER TABLE sensor_logs ADD COLUMN " + safeKey + " TEXT");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String buildInsertSql(Set<String> keys) {
        StringBuilder sb = new StringBuilder("INSERT INTO sensor_logs (timestamp");
        for (String key : keys) sb.append(", ").append(key.replaceAll("[^a-zA-Z0-9_]", "_"));
        sb.append(") VALUES (?");
        sb.append(", ?".repeat(keys.size()));
        sb.append(")");
        return sb.toString();
    }

    public record HistoryEntry(long t, String v) {
    }

    /**
     * Retrieves sensor values for a specific column between two timestamps.
     *
     * @param column  The name of the sensor column (e.g., "amdgpu_pci_0400_edge")
     * @param startTs Starting t
     * @param endTs   Ending t
     * @return Array of strings containing the sensor values
     */
    public static HistoryEntry[] getSensorDataRange(String column, long startTs, long endTs) {
        String safeColumn = column.replaceAll("[^a-zA-Z0-9_]", "");
        // Select both t and the desired column
        String sql = "SELECT timestamp, " + safeColumn + " FROM sensor_logs WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC";

        List<HistoryEntry> results = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, startTs);
            pstmt.setLong(2, endTs);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long ts = rs.getLong("timestamp");
                    String value = rs.getString(safeColumn);

                    // Combine them into a single string for your frontend
                    results.add(new HistoryEntry(ts, value));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results.toArray(new HistoryEntry[0]);
    }

    public static String getSensorColumn(SensorDevice device, SensorProperty prop) {
        return ((device.deviceName) + "__"
                + (device.adapter) + "__"
                + (prop.key)).replaceAll("[^a-zA-Z0-9_]", "_");
    }
}