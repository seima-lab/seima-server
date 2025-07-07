# Group Invitation Web Controller - FE Integration Guide

## Quick Summary

| Aspect | Details |
|--------|---------|
| **Endpoint** | `GET /invite/{inviteToken}` |
| **Purpose** | Xử lý invitation token và redirect đến app hoặc show error |
| **Success Result** | Redirect to Branch.io deep link |
| **Error Result** | Return HTML error template |
| **Templates** | `error_invalid_invitation.html`, `error_group_inactive.html` ✅ |

## URL Examples

```bash
# Development
http://localhost:8080/invite/abc123-def456-ghi789

# Production  
https://api.seima.app/invite/abc123-def456-ghi789
```

## Result Types & Actions

### ✅ Success Cases (Redirect to Branch Deep Link)
- **`STATUS_CHANGE_TO_PENDING_APPROVAL`** → User được thêm vào group với status PENDING
- **`ALREADY_PENDING_APPROVAL`** → User đã pending, show recheck message  
- **`ALREADY_ACTIVE_MEMBER`** → User đã là member, redirect vào group

### ❌ Error Cases (Show HTML Template)
- **`INVALID_OR_USED_TOKEN`** → Show `error_invalid_invitation.html`
- **`GROUP_INACTIVE_OR_DELETED`** → Show `error_group_inactive.html`

## Mobile App Integration

### 1. Handle Branch Deep Link
```javascript
import Branch from 'react-native-branch'

Branch.subscribe(({ error, params, uri }) => {
  if (params) {
    const { action, groupId } = params;
    
    switch(action) {
      case 'VIEW_GROUP':
        checkStatusAndNavigate(parseInt(groupId));
        break;
      case 'RECHECK_PENDING_STATUS':
        checkGroupMembershipStatus(parseInt(groupId));
        break;
    }
  }
});
```

### 2. Check Status API Before Navigate
**Endpoint**: `GET /api/v1/groups/{groupId}/my-status`

```javascript
const checkStatusAndNavigate = async (groupId) => {
  const response = await fetch(`/api/v1/groups/${groupId}/my-status`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  const { status, groupExists } = response.data;
  
  if (!groupExists) {
    showErrorAlert('Group not found');
    return;
  }
  
  switch(status) {
    case 'ACTIVE': 
      navigation.navigate('GroupDetail', { groupId });
      break;
    case 'PENDING_APPROVAL': 
      navigation.navigate('PendingApproval', { groupId });
      break;
    case null: 
      navigation.navigate('GroupInvitation', { groupId });
      break;
    default: 
      showErrorAlert('Cannot access this group');
  }
};
```

### 3. Status Response Structure
```typescript
interface GroupMemberStatusResponse {
  groupId: number;
  status: 'ACTIVE' | 'PENDING_APPROVAL' | 'INVITED' | 'REJECTED' | 'LEFT' | null;
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | null;
  groupExists: boolean;
}
```

## Testing Checklist

### Manual Testing:
- [ ] Valid token → Redirect to app  
- [ ] Invalid token → Show error page
- [ ] Inactive group → Show error page
- [ ] Already member → Redirect to app
- [ ] Mobile deep link → Call status API → Navigate correctly

### Error Templates:
- [ ] `error_invalid_invitation.html` - Có sẵn ✅
- [ ] `error_group_inactive.html` - Có sẵn ✅

## Key Files cho FE

1. **Controller**: `src/main/java/.../controller/GroupInvitationWebController.java`
2. **Templates**: `src/main/resources/templates/error_*.html` 
3. **API Docs**: Có sẵn trong docs folder
4. **Branch Config**: Cần `BRANCH_API_KEY` environment variable

## Flow Summary

```
User clicks link → Web Controller → Process token → 
Success: Redirect to Branch → App receives deep link → Check status API → Navigate
Error: Show HTML error page
```

**That's it! 🎯** FE team có đủ thông tin để merge và test. 