package com.fsl.quizapp.user.repository;

import com.fsl.quizapp.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link User}. */
public interface UserRepository extends JpaRepository<User, UUID> {
}
