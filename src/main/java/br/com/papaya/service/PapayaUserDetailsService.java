package br.com.papaya.service;

import br.com.papaya.model.User;
import br.com.papaya.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PapayaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public PapayaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.passwordHash())
                .roles(user.getRole().name())
                .build();
    }
}
