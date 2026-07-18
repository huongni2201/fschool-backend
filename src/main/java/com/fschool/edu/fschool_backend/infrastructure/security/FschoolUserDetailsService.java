package com.fschool.edu.fschool_backend.infrastructure.security;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RoleEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FschoolUserDetailsService implements UserDetailsService {

    private final UserJpaRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByPhoneOrUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User was not found"));
        return CustomUserDetails.builder()
                .id(user.getId())
                .username(user.getUsername() == null ? user.getPhone() : user.getUsername())
                .password(user.getPasswordHash())
                .role(roleCode(user))
                .status(user.getStatus())
                .build();
    }

    private String roleCode(UserEntity user) {
        RoleEntity role = user.getRole();
        if (role == null || role.getCode() == null || role.getCode().isBlank()) {
            throw new UsernameNotFoundException("User role is missing");
        }
        return role.getCode();
    }
}
