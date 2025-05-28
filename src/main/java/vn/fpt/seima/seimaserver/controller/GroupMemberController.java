package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.service.GroupMemberService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/group-members")
public class GroupMemberController {
    private GroupMemberService groupMemberService;
} 