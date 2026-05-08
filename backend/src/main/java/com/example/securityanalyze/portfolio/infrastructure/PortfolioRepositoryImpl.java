package com.example.securityanalyze.portfolio.infrastructure;

import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioRepository;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PortfolioRepositoryImpl implements PortfolioRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Portfolio> ROW_MAPPER = new RowMapper<>() {
        @Override
        public Portfolio mapRow(ResultSet rs, int rowNum) throws SQLException {
            Portfolio p = new Portfolio();
            p.setId(rs.getLong("id"));
            p.setUserId(rs.getLong("user_id"));
            p.setName(rs.getString("name"));
            p.setType(PortfolioType.valueOf(rs.getString("type")));
            p.setBroker(rs.getString("broker"));
            p.setDescription(rs.getString("description"));
            p.setIsDeleted(rs.getBoolean("is_deleted"));

            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) p.setDeletedAt(deletedAt.toLocalDateTime());

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) p.setCreatedAt(createdAt.toLocalDateTime());

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) p.setUpdatedAt(updatedAt.toLocalDateTime());

            return p;
        }
    };

    @Override
    public Portfolio save(Portfolio portfolio) {
        String sql = """
                INSERT INTO portfolio (user_id, name, type, broker, description, is_deleted, deleted_at, created_at, updated_at)
                VALUES (:userId, :name, :type::portfolio_type, :broker, :description, FALSE, NULL, NOW(), NOW())
                RETURNING id, user_id, name, type, broker, description, is_deleted, deleted_at, created_at, updated_at
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", portfolio.getUserId());
        params.addValue("name", portfolio.getName());
        params.addValue("type", portfolio.getType().name());
        params.addValue("broker", portfolio.getBroker());
        params.addValue("description", portfolio.getDescription());

        return jdbcTemplate.queryForObject(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<Portfolio> findById(Long id) {
        String sql = "SELECT * FROM portfolio WHERE id = :id AND is_deleted = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        List<Portfolio> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Portfolio> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT * FROM portfolio WHERE id = :id AND user_id = :userId AND is_deleted = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("userId", userId);
        List<Portfolio> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Portfolio> findByUserId(Long userId) {
        String sql = "SELECT * FROM portfolio WHERE user_id = :userId AND is_deleted = FALSE ORDER BY created_at DESC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public void update(Portfolio portfolio) {
        String sql = """
                UPDATE portfolio
                SET name = :name, broker = :broker, description = :description, updated_at = NOW()
                WHERE id = :id AND is_deleted = FALSE
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", portfolio.getId());
        params.addValue("name", portfolio.getName());
        params.addValue("broker", portfolio.getBroker());
        params.addValue("description", portfolio.getDescription());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void softDelete(Long id) {
        String sql = "UPDATE portfolio SET is_deleted = TRUE, deleted_at = NOW() WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void softDeleteByUserId(Long portfolioId, Long userId) {
        String sql = "UPDATE portfolio SET is_deleted = TRUE, deleted_at = NOW() WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", portfolioId);
        params.addValue("userId", userId);
        jdbcTemplate.update(sql, params);
    }
}
