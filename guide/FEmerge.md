# Frontend Integration Guide - Pending Groups APIs

## Overview
Hướng dẫn tích hợp 2 API mới cho tính năng quản lý pending groups:
1. **GET /api/v1/groups/pending** - Lấy danh sách groups đang chờ approval
2. **POST /api/v1/groups/cancel-join** - Hủy request join group

## 🚀 Quick Start

### 1. API Endpoints

```javascript
// Base URL
const BASE_URL = '/api/v1/groups';

// Endpoints
const PENDING_GROUPS_URL = `${BASE_URL}/pending`;
const CANCEL_JOIN_URL = `${BASE_URL}/cancel-join`;
```

### 2. Authentication
Tất cả API đều yêu cầu Bearer Token:
```javascript
const headers = {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
};
```

## 📋 API Details

### API 1: Get Pending Groups
**GET** `/api/v1/groups/pending`

#### Response Structure
```typescript
interface PendingGroupsResponse {
  status: number;
  message: string;
  data: PendingGroup[];
}

interface PendingGroup {
  groupId: number;
  groupName: string;
  groupAvatarUrl: string;
  groupIsActive: boolean;
  requestedAt: string; // ISO datetime
  activeMemberCount: number;
}
```

#### Example Response
```json
{
  "status": 200,
  "message": "User pending groups retrieved successfully",
  "data": [
    {
      "groupId": 1,
      "groupName": "Test Group 1",
      "groupAvatarUrl": "https://example.com/avatar1.jpg",
      "groupIsActive": true,
      "requestedAt": "2024-01-15T10:30:00",
      "activeMemberCount": 5
    }
  ]
}
```

### API 2: Cancel Join Request
**POST** `/api/v1/groups/cancel-join`

#### Request Body
```typescript
interface CancelJoinRequest {
  groupId: number;
}
```

#### Response Structure
```typescript
interface CancelJoinResponse {
  status: number;
  message: string;
  data: null;
}
```

#### Example Usage
```javascript
// Request
{
  "groupId": 1
}

// Response
{
  "status": 200,
  "message": "Join group request canceled successfully",
  "data": null
}
```

## 🔧 Implementation Guide

### 1. API Service Functions

```javascript
// api/groupService.js
class GroupService {
  constructor(baseURL, token) {
    this.baseURL = baseURL;
    this.token = token;
  }

  getHeaders() {
    return {
      'Authorization': `Bearer ${this.token}`,
      'Content-Type': 'application/json'
    };
  }

  // Get pending groups
  async getPendingGroups() {
    try {
      const response = await fetch(`${this.baseURL}/groups/pending`, {
        method: 'GET',
        headers: this.getHeaders()
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      return result.data; // Return array of pending groups
    } catch (error) {
      console.error('Error fetching pending groups:', error);
      throw error;
    }
  }

  // Cancel join request
  async cancelJoinRequest(groupId) {
    try {
      const response = await fetch(`${this.baseURL}/groups/cancel-join`, {
        method: 'POST',
        headers: this.getHeaders(),
        body: JSON.stringify({ groupId })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      return result;
    } catch (error) {
      console.error('Error canceling join request:', error);
      throw error;
    }
  }
}
```

### 2. React Component Example

```jsx
// components/PendingGroups.jsx
import React, { useState, useEffect } from 'react';
import { GroupService } from '../api/groupService';

const PendingGroups = ({ token }) => {
  const [pendingGroups, setPendingGroups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [cancelingGroupId, setCancelingGroupId] = useState(null);

  const groupService = new GroupService('/api/v1', token);

  // Fetch pending groups
  const fetchPendingGroups = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const groups = await groupService.getPendingGroups();
      setPendingGroups(groups);
    } catch (err) {
      setError('Failed to load pending groups');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Cancel join request
  const handleCancelRequest = async (groupId) => {
    if (!window.confirm('Are you sure you want to cancel this join request?')) {
      return;
    }

    setCancelingGroupId(groupId);
    
    try {
      await groupService.cancelJoinRequest(groupId);
      
      // Remove the canceled group from the list
      setPendingGroups(prev => prev.filter(group => group.groupId !== groupId));
      
      // Show success message
      alert('Join request canceled successfully');
    } catch (err) {
      alert('Failed to cancel join request: ' + err.message);
    } finally {
      setCancelingGroupId(null);
    }
  };

  useEffect(() => {
    fetchPendingGroups();
  }, []);

  if (loading) {
    return <div className="loading">Loading pending groups...</div>;
  }

  if (error) {
    return (
      <div className="error">
        <p>{error}</p>
        <button onClick={fetchPendingGroups}>Retry</button>
      </div>
    );
  }

  return (
    <div className="pending-groups">
      <h2>Pending Groups ({pendingGroups.length})</h2>
      
      {pendingGroups.length === 0 ? (
        <div className="empty-state">
          <p>No pending group requests</p>
        </div>
      ) : (
        <div className="groups-list">
          {pendingGroups.map(group => (
            <div key={group.groupId} className="group-item">
              <div className="group-info">
                <img 
                  src={group.groupAvatarUrl} 
                  alt={group.groupName}
                  className="group-avatar"
                />
                <div className="group-details">
                  <h3>{group.groupName}</h3>
                  <p>Requested: {new Date(group.requestedAt).toLocaleDateString()}</p>
                  <p>Members: {group.activeMemberCount}</p>
                </div>
              </div>
              
              <button
                onClick={() => handleCancelRequest(group.groupId)}
                disabled={cancelingGroupId === group.groupId}
                className="cancel-button"
              >
                {cancelingGroupId === group.groupId ? 'Canceling...' : 'Cancel Request'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default PendingGroups;
```

### 3. CSS Styling Example

```css
/* styles/PendingGroups.css */
.pending-groups {
  padding: 20px;
}

.groups-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
}

.group-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.group-details h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
}

.group-details p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.cancel-button {
  padding: 8px 16px;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-button:hover:not(:disabled) {
  background: #c82333;
}

.cancel-button:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.loading {
  text-align: center;
  padding: 40px;
}

.error {
  text-align: center;
  padding: 40px;
  color: #dc3545;
}
```

## 🧪 Testing

### 1. Manual Testing Checklist

#### Get Pending Groups API
- [ ] **Valid token**: Returns list of pending groups
- [ ] **Invalid token**: Returns 401 Unauthorized
- [ ] **No pending groups**: Returns empty array
- [ ] **Multiple pending groups**: Returns all groups with correct data

#### Cancel Join Request API
- [ ] **Valid request**: Successfully cancels and returns 200
- [ ] **Invalid groupId**: Returns 400 Bad Request
- [ ] **Non-existent group**: Returns 400 with "Group not found"
- [ ] **No pending request**: Returns 400 with "No pending join request found"
- [ ] **Invalid token**: Returns 401 Unauthorized

### 2. Error Handling

```javascript
// Error handling example
const handleApiError = (error) => {
  if (error.status === 401) {
    // Redirect to login
    router.push('/login');
  } else if (error.status === 400) {
    // Show error message
    showToast(error.message, 'error');
  } else {
    // Show generic error
    showToast('Something went wrong. Please try again.', 'error');
  }
};
```

## 🔄 Integration with Existing Code

### 1. Navigation Integration

```javascript
// Add to navigation menu
const navigationItems = [
  // ... existing items
  {
    label: 'Pending Groups',
    path: '/pending-groups',
    icon: 'clock',
    badge: pendingGroupsCount // Show count of pending groups
  }
];
```

### 2. State Management (Redux/Context)

```javascript
// Redux slice example
const pendingGroupsSlice = createSlice({
  name: 'pendingGroups',
  initialState: {
    groups: [],
    loading: false,
    error: null
  },
  reducers: {
    setPendingGroups: (state, action) => {
      state.groups = action.payload;
    },
    removePendingGroup: (state, action) => {
      state.groups = state.groups.filter(
        group => group.groupId !== action.payload
      );
    },
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
    setError: (state, action) => {
      state.error = action.payload;
    }
  }
});
```

### 3. Real-time Updates

```javascript
// If using WebSocket or polling
useEffect(() => {
  const interval = setInterval(() => {
    fetchPendingGroups();
  }, 30000); // Refresh every 30 seconds

  return () => clearInterval(interval);
}, []);
```

## 📱 Mobile Considerations

### 1. Touch-friendly UI

```css
/* Mobile-friendly button sizes */
.cancel-button {
  min-height: 44px; /* iOS minimum touch target */
  min-width: 44px;
  padding: 12px 20px;
}

.group-item {
  padding: 16px;
  margin-bottom: 8px;
}
```

### 2. Responsive Design

```css
@media (max-width: 768px) {
  .group-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .cancel-button {
    width: 100%;
  }
}
```

## 🚨 Common Issues & Solutions

### 1. CORS Issues
```javascript
// If encountering CORS issues, ensure backend allows your domain
// Backend should include:
// Access-Control-Allow-Origin: your-frontend-domain
// Access-Control-Allow-Headers: Authorization, Content-Type
```

### 2. Token Expiration
```javascript
// Handle token expiration gracefully
const handleTokenExpiration = () => {
  localStorage.removeItem('token');
  router.push('/login');
};

// Add to API service
if (response.status === 401) {
  handleTokenExpiration();
}
```

### 3. Network Errors
```javascript
// Add retry logic for network failures
const retryRequest = async (fn, retries = 3) => {
  try {
    return await fn();
  } catch (error) {
    if (retries > 0 && error.name === 'TypeError') {
      await new Promise(resolve => setTimeout(resolve, 1000));
      return retryRequest(fn, retries - 1);
    }
    throw error;
  }
};
```

## 📋 Deployment Checklist

- [ ] API endpoints are accessible from frontend domain
- [ ] CORS is properly configured
- [ ] Authentication tokens are being sent correctly
- [ ] Error handling is implemented
- [ ] Loading states are shown
- [ ] Success/error messages are displayed
- [ ] Mobile responsiveness is tested
- [ ] Integration with existing navigation is complete

## 📞 Support

Nếu gặp vấn đề trong quá trình tích hợp, vui lòng:
1. Kiểm tra Network tab trong DevTools để xem request/response
2. Kiểm tra Console để xem error messages
3. Verify token authentication
4. Contact backend team nếu cần hỗ trợ thêm

---

**Happy Coding! 🎉** 