package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.GroupMember;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {
} 