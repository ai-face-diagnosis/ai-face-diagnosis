package com.pdiagnosis.backend_api.userService.repositories;

import com.pdiagnosis.backend_api.userService.model.history.LLMRequestHistory;
import com.pdiagnosis.backend_api.userService.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LLMRequestHistoryRepository extends JpaRepository<LLMRequestHistory, Long> {
    List<LLMRequestHistory> findByUser(User user);
    Optional<LLMRequestHistory> findByImageHashAndUser(String imageHash, User user);
}
