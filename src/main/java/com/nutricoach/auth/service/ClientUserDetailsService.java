package com.nutricoach.auth.service;

import com.nutricoach.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("clientUserDetailsService")
@RequiredArgsConstructor
public class ClientUserDetailsService implements UserDetailsService {

    private final ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        boolean exists = clientRepository.existsByPhoneAndDeletedAtIsNull(phone);
        if (!exists) {
            throw new UsernameNotFoundException("Client not found: " + phone);
        }
        return User.builder()
                .username(phone)
                .password("")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CLIENT")))
                .build();
    }
}
