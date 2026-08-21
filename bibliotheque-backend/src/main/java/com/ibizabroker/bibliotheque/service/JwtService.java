package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class JwtService implements UserDetailsService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsersRepository userDao;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    public JwtResponse createJwtToken(JwtRequest jwtRequest) {
        String username = jwtRequest.username();
        String password = jwtRequest.password();
        authenticate(username, password);

        UserDetails userDetails = loadUserByUsername(username);
        String newGeneratedToken = jwtUtil.generateToken(userDetails);

        Users user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new JwtResponse(user, newGeneratedToken);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // NB : l'ancien code faisait .get() avant de tester "user != null", ce qui
        // levait une NoSuchElementException (non geree) au lieu du
        // UsernameNotFoundException attendu par Spring Security si l'utilisateur
        // n'existait pas -> le bloc "else" ci-dessous n'etait jamais atteint.
        Users user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                getAuthority(user)
        );
    }

    private Set getAuthority(Users user) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        user.getRole().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
        });
        return authorities;
    }

    // Ne catch plus DisabledException/BadCredentialsException pour les emballer dans
    // une Exception generique : ce sont deja des AuthenticationException (non
    // checked). Les laisser se propager permet a ExceptionTranslationFilter de les
    // intercepter et de delegue a JwtAuthenticationEntryPoint -> 401 propre, au lieu
    // de remonter jusqu'au DispatcherServlet en tant qu'Exception non geree (500).
    private void authenticate(String userName, String userPassword) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, userPassword));
    }
}