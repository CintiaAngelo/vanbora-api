package com.vanbora.api.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ajustes de schema que o {@code ddl-auto: update} não faz sozinho. Idempotente: cada
 * patch só age se o banco ainda estiver no formato antigo, e roda antes dos seeders.
 *
 * <p>Hibernate 6 mapeia {@code @Enumerated(STRING)} para o tipo {@code enum(...)} nativo
 * do MySQL ao CRIAR a coluna, mas o {@code update} nunca altera colunas existentes. Um
 * valor novo no enum Java (ex.: {@code UserRole.MONITOR}) seria então recusado pelo banco
 * ("Data truncated"). Aqui essas colunas viram {@code VARCHAR}, que aceita qualquer valor
 * do enum — a validação continua no Java.
 */
@Configuration
public class SchemaPatches {

    private static final Logger log = LoggerFactory.getLogger(SchemaPatches.class);

    /** (tabela, coluna, tamanho) das colunas de enum que ganharam valores novos. */
    private static final List<String[]> ENUM_COLUMNS = List.<String[]>of(
            new String[] {"users", "role", "20"});

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ApplicationRunner applySchemaPatches(DataSource dataSource) {
        return args -> {
            if (!isMySql(dataSource)) {
                return; // H2 dos testes nasce do zero (create-drop) com todos os valores.
            }
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            for (String[] column : ENUM_COLUMNS) {
                widenEnumColumn(jdbc, column[0], column[1], column[2]);
            }
        };
    }

    private void widenEnumColumn(JdbcTemplate jdbc, String table, String column, String length) {
        String dataType = jdbc.query(
                "select data_type from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                table, column);
        if (!"enum".equalsIgnoreCase(dataType)) {
            return;
        }
        jdbc.execute("alter table `%s` modify `%s` varchar(%s) not null".formatted(table, column, length));
        log.info("Schema: {}.{} convertida de enum para varchar({}).", table, column, length);
    }

    private boolean isMySql(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            return meta.getDatabaseProductName().toLowerCase().contains("mysql");
        } catch (Exception e) {
            log.warn("Não foi possível identificar o banco para os patches de schema: {}", e.getMessage());
            return false;
        }
    }
}
