package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.service.GroupService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {
    private GroupService groupService;
} 