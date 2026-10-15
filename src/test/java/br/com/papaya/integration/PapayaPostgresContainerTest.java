
package br.com.papaya.integration;

import br.com.papaya.dto.request.ProductRequest;
import br.com.papaya.dto.response.ProductResponse;
import br.com.papaya.enums.ProductSource;
import br.com.papaya.enums.VerificationStatus;
import br.com.papaya.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.postgresql.util.PSQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(PapayaPostgresTestConfiguration.class)
class PapayaPostgresContainerTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deveConectarAoPostgresDoContainer() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(2));
        }
    }

    @Test
    void deveExecutarMigrationDoFlyway() {
        Integer quantidade = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1'",
                Integer.class
        );

        assertTrue(quantidade != null && quantidade > 0);
    }

    @Test
    @Transactional
    void devePersistirERecuperarProdutoNoPostgresDoContainer() {
        ProductResponse criado = productService.create(new ProductRequest("  Café especial  "));
        entityManager.flush();
        entityManager.clear();

        ProductResponse recuperado = productService.findById(criado.id());

        assertNotNull(recuperado.id());
        assertEquals("Café especial", recuperado.name());
        assertEquals(ProductSource.USER, recuperado.source());
        assertEquals(VerificationStatus.PENDING_REVIEW, recuperado.verificationStatus());
        assertNotNull(recuperado.createdAt());
        assertNotNull(recuperado.updatedAt());
    }

    @Test
    void deveRejeitarNomeEmBrancoPelaConstraintDaV3() {
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> inserirProduto("   ", "USER", "PENDING_REVIEW")
        );

        assertConstraintViolation(exception, "products_name_not_blank");
    }

    @Test
    void deveRejeitarSourceInvalidoPelaConstraintDaV4() {
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> inserirProduto("Produto válido", "INVALID_SOURCE", "PENDING_REVIEW")
        );

        assertConstraintViolation(exception, "products_source_valid");
    }

    @Test
    void deveRejeitarVerificationStatusInvalidoPelaConstraintDaV4() {
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> inserirProduto("Produto válido", "USER", "INVALID_STATUS")
        );

        assertConstraintViolation(exception, "products_verification_status_valid");
    }

    private void inserirProduto(String name, String source, String verificationStatus) {
        jdbcTemplate.update("""
                INSERT INTO products (name, source, verification_status, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, name, source, verificationStatus);
    }

    private void assertConstraintViolation(DataIntegrityViolationException exception, String constraintName) {
        PSQLException postgresException = findPostgresException(exception);
        assertEquals("23514", postgresException.getSQLState());
        assertEquals(constraintName, postgresException.getServerErrorMessage().getConstraint());
    }

    private PSQLException findPostgresException(Throwable exception) {
        Throwable cause = exception;
        while (cause != null && !(cause instanceof PSQLException)) {
            cause = cause.getCause();
        }
        assertTrue(cause instanceof PSQLException, "A falha deve ser reportada pelo driver PostgreSQL");
        return (PSQLException) cause;
    }
}
