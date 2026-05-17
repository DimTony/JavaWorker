package com.owlite.worker.repository;

import com.owlite.worker.config.DbConfig;
import com.owlite.worker.model.Finding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class FindingPersistenceService {

    private static final String INSERT_FINDING = """
        INSERT INTO "Findings" ("Id", "ScanId", "Surface", "Severity", "Title", "CveId",
            "AiExplanation", "TechnicalPayload", "RemediationSteps", "Status", "CreatedAt")
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Open', ?)
        """;

    private static final String UPDATE_SCAN = """
        UPDATE "Scans"
        SET "Status" = 'Completed', "SecurityScore" = ?, "CompletedAt" = ?, "UpdatedAt" = ?
        WHERE "Id" = ?
        """;

    public void saveFindings(String scanId, List<Finding> findings, int securityScore) {
        try (Connection conn = DbConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertFindings(conn, findings);
                updateScan(conn, scanId, securityScore);
                conn.commit();
                System.out.printf("Saved %d findings for scan %s%n", findings.size(), scanId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Failed to save findings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertFindings(Connection conn, List<Finding> findings) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_FINDING)) {
            for (Finding f : findings) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, UUID.fromString(f.scanId()));
                stmt.setString(3, f.surface());
                stmt.setString(4, f.severity());
                stmt.setString(5, f.title());
                stmt.setString(6, f.cveId());
                stmt.setString(7, f.aiExplanation());
                stmt.setString(8, f.technicalPayload());
                stmt.setString(9, f.remediationSteps());
                stmt.setTimestamp(10, Timestamp.from(Instant.now()));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void updateScan(Connection conn, String scanId, int securityScore) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SCAN)) {
            Timestamp now = Timestamp.from(Instant.now());
            stmt.setInt(1, securityScore);
            stmt.setTimestamp(2, now);
            stmt.setTimestamp(3, now);
            stmt.setObject(4, UUID.fromString(scanId));
            stmt.executeUpdate();
        }
    }
}