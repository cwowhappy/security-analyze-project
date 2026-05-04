package com.example.securityanalyze.user.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(UserRepositoryImpl.class)
class UserRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldInsertNewUser() {
        User toSave = TestDataFactory.user("newuser", UserStatus.PENDING);

        User saved = userRepository.save(toSave);

        assertNotNull(saved.getId());
        assertEquals("newuser", saved.getUsername());
        assertEquals(UserStatus.PENDING, saved.getStatus());
        assertEquals(Role.USER, saved.getRole());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldUpdateExistingUserOnConflict() {
        User first = userRepository.save(TestDataFactory.user("dupuser", UserStatus.PENDING));
        Long originalId = first.getId();

        User second = TestDataFactory.user("dupuser", UserStatus.APPROVED);
        second.setPasswordHash("new_hash");
        User updated = userRepository.save(second);

        assertEquals(originalId, updated.getId());
        assertEquals(UserStatus.APPROVED, updated.getStatus());
        assertEquals("new_hash", updated.getPasswordHash());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void shouldFindByUsername() {
        userRepository.save(TestDataFactory.user("finder", UserStatus.APPROVED));

        Optional<User> found = userRepository.findByUsername("finder");

        assertTrue(found.isPresent());
        assertEquals("finder", found.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        Optional<User> found = userRepository.findByUsername("nobody");

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldCheckExistsByUsername() {
        userRepository.save(TestDataFactory.user("exists", UserStatus.APPROVED));

        assertTrue(userRepository.existsByUsername("exists"));
        assertFalse(userRepository.existsByUsername("not_exists"));
    }

    @Test
    void shouldFindAllUsersOrderedByCreatedAtDesc() {
        userRepository.save(TestDataFactory.user("u1", UserStatus.APPROVED));
        userRepository.save(TestDataFactory.user("u2", UserStatus.PENDING));

        List<User> all = userRepository.findAll();

        assertEquals(2, all.size());
        assertEquals("u2", all.get(0).getUsername());
        assertEquals("u1", all.get(1).getUsername());
    }

    @Test
    void shouldUpdateStatus() {
        User saved = userRepository.save(TestDataFactory.user("statuser", UserStatus.PENDING));

        userRepository.updateStatus(saved.getId(), UserStatus.APPROVED);

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(UserStatus.APPROVED, found.get().getStatus());
    }

    @Test
    void shouldFindById() {
        User saved = userRepository.save(TestDataFactory.user("idfinder", UserStatus.APPROVED));

        Optional<User> found = userRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("idfinder", found.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenIdNotFound() {
        Optional<User> found = userRepository.findById(99999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindByStatus() {
        userRepository.save(TestDataFactory.user("pending_user", UserStatus.PENDING));
        userRepository.save(TestDataFactory.user("approved_user", UserStatus.APPROVED));
        userRepository.save(TestDataFactory.user("disabled_user", UserStatus.DISABLED));

        List<User> pendingUsers = userRepository.findByStatus(UserStatus.PENDING);
        List<User> approvedUsers = userRepository.findByStatus(UserStatus.APPROVED);

        assertTrue(pendingUsers.stream().anyMatch(u -> "pending_user".equals(u.getUsername())));
        assertTrue(approvedUsers.stream().anyMatch(u -> "approved_user".equals(u.getUsername())));
        assertTrue(pendingUsers.stream().noneMatch(u -> "approved_user".equals(u.getUsername())));
    }

    @Test
    void shouldPreserveCreatedAtOnConflictUpdate() {
        User first = userRepository.save(TestDataFactory.user("preserve_user", UserStatus.PENDING));
        java.time.LocalDateTime originalCreatedAt = first.getCreatedAt();

        // 等待一小段时间确保 updated_at 可能不同
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        User second = TestDataFactory.user("preserve_user", UserStatus.APPROVED);
        User updated = userRepository.save(second);

        assertEquals(originalCreatedAt, updated.getCreatedAt(), "冲突更新时应保留原始 created_at");
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void shouldDoNothingWhenUpdateStatusForNonExistingId() {
        // 对不存在的 ID 更新状态应静默完成，不抛异常
        assertDoesNotThrow(() -> userRepository.updateStatus(99999L, UserStatus.APPROVED));
    }
}
