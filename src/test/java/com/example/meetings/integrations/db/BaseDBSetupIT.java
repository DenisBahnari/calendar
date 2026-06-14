package com.example.meetings.integrations.db;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseDBSetupIT {

    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    void cleanDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");

            stmt.execute("DELETE FROM meeting_participants");
            stmt.execute("DELETE FROM meetings");
            stmt.execute("DELETE FROM users");

            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    protected void execute(Operation operation) {
        new DbSetup(new DataSourceDestination(dataSource), operation).launch();
    }
}