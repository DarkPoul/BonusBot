package esvar.ua.bonusbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import esvar.ua.bonusbot.model.entity.SessionEntity;
import esvar.ua.bonusbot.model.enums.SessionState;
import esvar.ua.bonusbot.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    public SessionService(SessionRepository sessionRepository, ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SessionEntity get(Long userId) {
        return sessionRepository.findById(userId).orElse(null);
    }

    @Transactional
    public void set(Long userId, SessionState state, Map<String, Object> payload) {
        SessionEntity session = sessionRepository.findById(userId).orElseGet(SessionEntity::new);
        session.setUserId(userId);
        session.setState(state);
        session.setPayloadJson(write(payload));
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void clear(Long userId) {
        sessionRepository.deleteById(userId);
    }

    public Map<String, Object> getPayload(SessionEntity session) {
        if (session == null || session.getPayloadJson() == null || session.getPayloadJson().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(session.getPayloadJson(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private String write(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Collections.emptyMap() : payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
