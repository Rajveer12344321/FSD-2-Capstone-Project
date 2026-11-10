package com.bloodbridge.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Hosts such as Render and Railway provide DATABASE_URL as postgres://user:pass@host:port/db.
 * This converts it to a JDBC DataSource. It is only active when DATABASE_URL is set.
 */
@Configuration
public class DatabaseUrlConfig {

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource(@Value("${DATABASE_URL}") String databaseUrl) throws Exception {
        URI uri = new URI(databaseUrl.replaceFirst("^postgres(ql)?://", "postgresql://"));
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath());
        if (uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            ds.setUsername(parts[0]);
            if (parts.length > 1) ds.setPassword(parts[1]);
        }
        return ds;
    }
}
