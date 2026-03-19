package com.nutricoach.auth.service;

import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoachUserDetailsService implements UserDetailsService {

    private final CoachRepository coachRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        Coach coach = coachRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("Coach not found: " + phone));

        return User.builder()
                .username(coach.getPhone())
                .password("")   // password-less — auth is OTP → JWT
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_COACH")))
                .build();
    }
}
