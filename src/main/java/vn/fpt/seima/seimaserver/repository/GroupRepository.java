package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Group;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    
    /**
     * Find group by invitation code
     * @param inviteCode the invitation code
     * @return Optional of Group
     */
    Optional<Group> findByGroupInviteCode(String inviteCode);
    
    /**
     * Find active group by invitation code
     * @param inviteCode the invitation code
     * @param isActive the active status
     * @return Optional of Group
     */
    @Query("SELECT g FROM Group g WHERE g.groupInviteCode = :inviteCode AND g.groupIsActive = :isActive")
    Optional<Group> findByGroupInviteCodeAndGroupIsActive(@Param("inviteCode") String inviteCode, 
                                                          @Param("isActive") Boolean isActive);
    
    /**
     * Check if group exists by invitation code and is active
     * @param inviteCode the invitation code
     * @param isActive the active status
     * @return true if exists and active, false otherwise
     */
    boolean existsByGroupInviteCodeAndGroupIsActive(String inviteCode, Boolean isActive);
} 