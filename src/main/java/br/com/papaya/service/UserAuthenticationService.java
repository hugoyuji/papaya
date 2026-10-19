package br.com.papaya.service;

import br.com.papaya.dto.request.RegisterRequest;
import br.com.papaya.dto.response.UserResponse;
import br.com.papaya.model.User;
import br.com.papaya.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String email = request.email().strip().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        try {
            return toResponse(userRepository.saveAndFlush(new User(
                    email, request.displayName(), passwordEncoder.encode(request.password()))));
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT))
                .map(UserAuthenticationService::toResponse)
                .orElseThrow();
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.getContributorTrustStatus());
    }
}
