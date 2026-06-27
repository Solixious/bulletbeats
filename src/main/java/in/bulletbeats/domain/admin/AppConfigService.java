package in.bulletbeats.domain.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final JdbcTemplate jdbcTemplate;

    public String get(String key, String defaultValue) {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT value FROM app_config WHERE key = ?", String.class, key);
            return value != null ? value : defaultValue;
        } catch (EmptyResultDataAccessException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        jdbcTemplate.update(
                "UPDATE app_config SET value = ?, updated_at = now() WHERE key = ?",
                value, key);
    }
}
