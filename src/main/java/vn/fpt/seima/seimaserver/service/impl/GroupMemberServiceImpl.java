package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.service.GroupMemberService;

@Service
@AllArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {
    private GroupMemberRepository groupMemberRepository;
} 