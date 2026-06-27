package in.bulletbeats.domain.user;

import in.bulletbeats.domain.shared.enums.Role;
import in.bulletbeats.domain.user.entity.User;
import in.bulletbeats.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager entityManager;

    // Flush writes to DB within the transaction, clear evicts the first-level
    // cache so subsequent finds hit actual SQL rather than the identity map.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private User buildUser(String username, String fullName, Role role) {
        return User.builder()
                .username(username)
                .passwordHash("$2a$10$placeholder.hash.for.testing.purposes.only")
                .fullName(fullName)
                .role(role)
                .isActive(true)
                .tenantId(1L)
                .build();
    }

    @Test
    void save_assignsIdAndPopulatesAuditFields() {
        User user = buildUser("jdoe", "John Doe", Role.STAFF);

        User saved = userRepository.save(user);
        flushAndClear();

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByUsername_returnsUserWithAllFields() {
        userRepository.save(buildUser("jdoe", "John Doe", Role.STAFF));
        flushAndClear();

        Optional<User> result = userRepository.findByUsername("jdoe");

        assertThat(result).isPresent();
        User found = result.get();
        assertThat(found.getUsername()).isEqualTo("jdoe");
        assertThat(found.getFullName()).isEqualTo("John Doe");
        assertThat(found.getRole()).isEqualTo(Role.STAFF);
        assertThat(found.isEnabled()).isTrue();
        assertThat(found.getPasswordHash()).isNotBlank();
    }

    @Test
    void findByUsername_returnsEmptyForUnknownUsername() {
        Optional<User> result = userRepository.findByUsername("nobody");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsername_returnsTrueForSavedUser() {
        userRepository.save(buildUser("jdoe", "John Doe", Role.STAFF));
        flushAndClear();

        assertThat(userRepository.existsByUsername("jdoe")).isTrue();
    }

    @Test
    void existsByUsername_returnsFalseForAbsentUsername() {
        assertThat(userRepository.existsByUsername("ghost")).isFalse();
    }

    @Test
    void findAllByOrderByFullNameAsc_returnsUsersAlphabetically() {
        userRepository.save(buildUser("zsmith", "Zoe Smith", Role.STAFF));
        userRepository.save(buildUser("aadams", "Alice Adams", Role.MANAGER));
        userRepository.save(buildUser("bmiller", "Bob Miller", Role.ADMIN));
        flushAndClear();

        List<User> users = userRepository.findAllByOrderByFullNameAsc();

        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getFullName)
                .containsExactly("Alice Adams", "Bob Miller", "Zoe Smith");
    }

    @Test
    void findAllByOrderByFullNameAsc_returnsEmptyListWhenNoUsers() {
        List<User> users = userRepository.findAllByOrderByFullNameAsc();

        assertThat(users).isEmpty();
    }
}
