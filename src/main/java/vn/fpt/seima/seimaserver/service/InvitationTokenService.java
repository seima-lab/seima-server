package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.InvitationTokenData;

import java.util.Optional;


public interface InvitationTokenService {
    

    String createInvitationToken(InvitationTokenData tokenData);
    

    Optional<InvitationTokenData> getInvitationTokenData(String token);
    

    boolean updateInvitationTokenStatus(String token, String newStatus);
    

    boolean removeInvitationToken(String token);
    

    boolean removeInvitationTokenByUserAndGroup(Integer userId, Integer groupId);


    String generateTokenKey(String token);
    

    String generateTokenKeyByUserAndGroup(Integer userId, Integer groupId);
} 