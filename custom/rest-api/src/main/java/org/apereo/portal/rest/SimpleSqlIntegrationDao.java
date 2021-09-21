package org.apereo.portal.rest;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.PooledConnection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Simple DAO that provides a generic method to run a prepared statement, returning an array of maps.
 */
@Slf4j
@Component
public class SimpleSqlIntegrationDao {

    @Autowired
    @Qualifier("IntegrationsDb")
    private DataSource ds;

    /**
     * Generic method to run a SQL select statement with parameters. The results are returned as a list of maps.
     *
     * @param sql SQL Select string with placeholders for each params value
     * @param params values in order to match placeholders in sql string
     * @return Array of maps where each map represents a record
     */
    List<Map<String, Object>> getRecords(final String sql, String... params) {
        log.debug("sql = {}", sql);
        log.debug("params = {}", params.toString());
        PooledConnection conn;
        try {
            conn = ds.getPooledConnection();
        } catch (SQLException se) {
            log.error("Could not obtain datasource for integration database", se);
            return Collections.emptyList();
        }
        JdbcTemplate template = new JdbcTemplate(ds);
        // slow and failed queries should show up in catalina.out log from Slow Query Interceptor for Tomcat Pool
        List<Map<String, Object>> recs = template.queryForList(sql, params);
        log.debug("returned record count: {}", recs.size());
        log.debug("{}", recs.toString());
        return recs;
    }
}
