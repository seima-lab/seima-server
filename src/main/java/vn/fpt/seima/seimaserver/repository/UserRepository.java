package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.User;



@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUserPhoneNumber(String phoneNumber);
}
