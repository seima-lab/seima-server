package vn.fpt.seima.seimaserver.repository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.User;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUserPhoneNumber(String phoneNumber);


    Optional<User> findByUserEmail(String currentEmail);

    // This method will find a user by email and check if the user is active
    Optional<User> findByUserEmailAndUserIsActiveTrue(@Email @NotBlank String email);

    // I want write query to find user by email and isActive is false
    Optional<User> findByUserEmailAndUserIsActiveFalse(@Email @NotBlank String email);

}
