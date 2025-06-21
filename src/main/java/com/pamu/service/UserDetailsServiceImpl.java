package com.pamu.service;

import com.pamu.model.User;
import com.pamu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired UserRepository userRepository;

    /**
     * Loads the user details for Spring Security authentication by username.
     * This method is developed to integrate with Spring Security and provide user lookup for authentication and authorization.
     * Throws UsernameNotFoundException if the user does not exist in the system.
     *
     * @param username the username identifying the user whose data is required
     * @return UserDetails for the given username
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);
        return user.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
