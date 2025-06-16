package vn.fpt.seima.seimaserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.exception.UserAccountNotActiveException;
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

        // Kiểm tra xem user có active không (chỉ cho phép user active sử dụng JWT)
        if (!appUser.getUserIsActive()) {
            throw new UserAccountNotActiveException("User account is not active: " + email);
        }

        // Convert AppUser to Spring Security UserDetails
        // No roles/authorities needed for this application
        return new User(appUser.getUserEmail(), "", new ArrayList<>()); // Password field is not used for JWT auth here
    }
}
