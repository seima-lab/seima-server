package vn.fpt.seima.seimaserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import java.util.ArrayList;

@Service
public class AppUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        vn.fpt.seima.seimaserver.entity.User appUser = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Convert AppUser to Spring Security UserDetails
        // For simplicity, no roles/authorities are added here. Add them as needed.
        return new User(appUser.getUserEmail(), "", new ArrayList<>()); // Password field is not used for JWT auth here
    }
}
