package in.bulletbeats.domain.user.repository;

import in.bulletbeats.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findAllByOrderByFullNameAsc();

    List<User> findAllByIsActiveTrue();

    boolean existsByUsername(String username);
}
