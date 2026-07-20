package dev.monorepo.apps.progen.repository;

import dev.monorepo.apps.progen.model.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<@NonNull User, @NonNull String> {
    Optional<User> findUserByUsername(String username);
}
