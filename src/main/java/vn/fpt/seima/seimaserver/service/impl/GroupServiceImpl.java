package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.GroupService;

@Service
@AllArgsConstructor
public class GroupServiceImpl implements GroupService {
    private GroupRepository groupRepository;
} 