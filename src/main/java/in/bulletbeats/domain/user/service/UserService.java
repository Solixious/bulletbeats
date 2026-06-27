package in.bulletbeats.domain.user.service;

import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.shared.exception.UsernameAlreadyExistsException;
import in.bulletbeats.domain.user.entity.User;
import in.bulletbeats.domain.user.dto.CreateUserDto;
import in.bulletbeats.domain.user.dto.UpdateUserDto;
import in.bulletbeats.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public User createUser(CreateUserDto dto, Long createdByUserId) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UsernameAlreadyExistsException(dto.getUsername());
        }
        User user = User.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .role(dto.getRole())
                .isActive(true)
                .tenantId(1L)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UpdateUserDto dto) {
        User user = getUserById(id);
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        user.setActive(dto.isActive());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long id, String newRawPassword) {
        User user = getUserById(id);
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    public Map<Long, String> getUsernameMap() {
        return userRepository.findAllByOrderByFullNameAsc().stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public boolean isSetupCompleted() {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT value FROM app_config WHERE key = 'setup.completed'",
                    String.class);
            return Boolean.parseBoolean(value);
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    @Transactional
    public void markSetupCompleted() {
        jdbcTemplate.update(
                "UPDATE app_config SET value = 'true', updated_at = now() WHERE key = 'setup.completed'");
    }
}
