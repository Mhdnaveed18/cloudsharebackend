# CloudShareOriginal — Complete API Reference

This document lists all available REST APIs, their request payloads, headers, and example responses. All responses are wrapped in a common envelope EntityResponse<T>.

Common response envelope
```json
{
  "success": true,
  "message": "...",
  "data": {},
  "timestamp": "2025-08-13T02:22:00Z",
  "path": "/api/...",
  "errors": null
}
```

- success: boolean indicating operation result
- message: human-readable message
- data: endpoint-specific payload (may be null)
- timestamp: ISO-8601 UTC time
- path: request URI
- errors: map of validation or field errors (present on validation failures)

Authentication
- Public endpoints: /api/auth/**
- All other endpoints require JWT Bearer token in Authorization header
- Header for protected endpoints: Authorization: Bearer <accessToken>

Base URL
- Local: http://localhost:8080

---

## 1. Auth APIs (/api/auth)

### 1.1 Register
- Method: POST
- Path: /api/auth/register
- Body (RegisterRequest):
```json
{
  "email": "user@example.com",
  "password": "Password123!",  
  "firstName": "John",
  "lastName": "Doe"
}
```
- Validations:
  - email: not blank, valid email
  - password: not blank, min length 8
  - firstName: not blank
  - lastName: not blank
- Response (201 Created) data (AuthResponse):
```json
{ "accessToken": "<JWT>" }
```
- Error cases:
  - 409 CONFLICT: {"message":"Email already registered"}
  - 400 BAD REQUEST: validation errors in errors map

Example cURL
```
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!","firstName":"John","lastName":"Doe"}'
```

### 1.2 Login
- Method: POST
- Path: /api/auth/login
- Body (LoginRequest):
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```
- Success response data (AuthResponse):
```json
{ "accessToken": "<JWT>" }
```
- Example of the full envelope (matches your earlier sample):
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "<JWT>"
  },
  "timestamp": "2025-08-13T02:22:00Z",
  "path": "/api/auth/login",
  "errors": null
}
```
- Error cases:
  - 401 UNAUTHORIZED: {"message":"Invalid credentials"}
  - 403 FORBIDDEN: {"message":"Email not verified"}

### 1.3 Verify Email
- Method: POST
- Path: /api/auth/verify
- Body (VerifyEmailRequest):
```json
{
  "email": "user@example.com",
  "verificationCode": "a1b2c3"  
}
```
- Response (200 OK): message "Email verified successfully"
- Error cases: 400 BAD REQUEST (invalid/expired code or email)

### 1.4 Forgot Password
- Method: POST
- Path: /api/auth/forgot-password
- Body (ForgotPasswordRequest):
```json
{ "email": "user@example.com" }
```
- Response data (ForgotPasswordResponse):
```json
{
  "message": "If that account exists, a reset link has been generated.",
  "resetToken": "<token-or-null>"
}
```
- Note: resetToken may be returned for development/testing.

### 1.5 Reset Password
- Method: POST
- Path: /api/auth/reset-password
- Body (ResetPasswordRequest):
```json
{
  "token": "<RESET_TOKEN>",
  "newPassword": "NewPassword123!"
}
```
- Response: message "Password reset successful"
- Error cases: 400 BAD REQUEST (invalid/expired token, validation)

---

## 2. File APIs (/api/files)
All File APIs require Authorization: Bearer <accessToken> except where explicitly stated (all below require auth).

### 2.1 Upload a file (multipart)
- Method: POST
- Path: /api/files/upload
- Content-Type: multipart/form-data
- Fields:
  - file: the file content
- Success response data (UploadResponse):
```json
{
  "id": 123,
  "name": "your.pdf",
  "contentType": "application/pdf",
  "size": 34567,
  "visibility": "PRIVATE",
  "fileUrl": "<s3KeyOrUrl>"
}
```
- Error cases:
  - 400 BAD REQUEST: {"message":"File too large (max <bytes> bytes)"}

PowerShell example
```
$FilePath = "C:\\path\\to\\your.pdf"
$token = "<JWT>"
Invoke-RestMethod -Uri "http://localhost:8080/api/files/upload" -Method Post \
  -Headers @{ Authorization = "Bearer $token" } \
  -Form @{ file = Get-Item $FilePath }
```

cURL example
```
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <JWT>" \
  -F "file=@/path/to/your.pdf"
```

### 2.2 List files
- Method: GET
- Path: /api/files
- Optional query: visibility=PUBLIC|PRIVATE
- Success response data: array of FileItem (full entity as returned by service). Each item includes id, originalName, contentType, size, visibility, s3Key, etc.

Example
```
curl -H "Authorization: Bearer <JWT>" http://localhost:8080/api/files
```

### 2.3 Change visibility
- Method: PATCH
- Path: /api/files/{id}/visibility
- Body (ToggleVisibilityRequest):
```json
{ "visibility": "PUBLIC" }
```
- Success response data (FileSummaryResponse):
```json
{
  "id": 123,
  "name": "your.pdf",
  "contentType": "application/pdf",
  "size": 34567,
  "visibility": "PUBLIC",
  "fileUrl": "<s3KeyOrUrl>"
}
```

### 2.4 Delete file
- Method: DELETE
- Path: /api/files/{id}
- Response: 204 No Content on success

### 2.5 View file (metadata + URL rules)
- Method: GET
- Path: /api/files/{id}/view
- Behavior:
  - If PUBLIC: returns FileSummaryResponse with fileUrl for any authenticated user
  - If PRIVATE and requester is owner: returns fileUrl
  - If PRIVATE and requester is not owner: fileUrl is null and message indicates private access
- Success response data (FileSummaryResponse), with fileUrl possibly null depending on access

### 2.6 Get download URL
- Method: GET
- Path: /api/files/{id}/download-url
- Response data (DownloadUrlResponse):
```json
{ "downloadUrl": "<s3KeyOrUrl-or-null>" }
```
- success flag is false and message is explanatory if access is not allowed

### 2.7 List files by user
- Method: GET
- Path: /api/files/user/{userId}
- Behavior:
  - If requesting your own userId: returns all your files; otherwise only PUBLIC files
- Success response data: array of FileSummaryResponse

### 2.8 Get quota
- Method: GET
- Path: /api/files/quota
- Response data (QuotaResponse):
```json
{ "used": 3, "limit": 5, "remaining": 2, "plan": "inactive" }
```

---

## 3. Billing APIs (/api/billing)
All Billing APIs require Authorization: Bearer <accessToken>.

Configuration defaults
- app.subscription.file-limit: 100 (files)
- app.subscription.price-inr: 500 (INR)

### 3.1 Create payment order (Razorpay one-time)
- Method: POST
- Path: /api/billing/payment/order
- Response data (PaymentOrderResponse):
```json
{
  "orderId": "order_123",
  "amount": 50000,            
  "currency": "INR",
  "razorpayKey": "rzp_test_xxxxx"
}
```
- Notes:
  - amount is in paise (e.g., ₹500 -> 50000 paise) depending on configuration

### 3.2 Verify payment
- Method: POST
- Path: /api/billing/payment/verify
- Body (PaymentVerifyRequest):
```json
{
  "orderId": "order_123",
  "paymentId": "pay_ABCDEF",
  "signature": "<razorpay_signature>"
}
```
- Success response: message "Payment verified. Quota upgraded." with data: "ok"
- Failure response: 400 BAD REQUEST with message "Invalid payment signature" and data: "failed"

### 3.3 Billing status (current user)
- Method: GET
- Path: /api/billing/status
- Response data (QuotaResponse):
```json
{
  "used": 3,
  "limit": 100,
  "remaining": 97,
  "plan": "active"
}
```
- message explains whether Pro plan is active or Free plan applies

---

## 4. Error handling conventions
- Validation errors: 400 BAD REQUEST with errors map per field
- Unauthorized: 401 UNAUTHORIZED with message "Invalid credentials" (for auth failures)
- Access denied: 403 FORBIDDEN with message "Access denied" (from security) or private file access messages in file endpoints
- Not found: 404 NOT FOUND for missing resources (e.g., file not found in some cases)
- Generic server errors: 500 INTERNAL SERVER ERROR with message "Internal server error"

---

## 5. Quick Postman/cURL notes
- Use Authorization: Bearer <accessToken> for all non-/api/auth endpoints
- Login flow for frontend toast UI:
  - On HTTP 200 with success=true and message="Login successful", you can show a success toast and store data.accessToken
- Sample PowerShell to call protected endpoint:
```
$token = "<JWT>"
Invoke-RestMethod -Uri "http://localhost:8080/api/files/quota" -Headers @{ Authorization = "Bearer $token" }
```

---

## User APIs (/api/user)
These endpoints help fetch user profile details and user emails for selection.

### Get current user profile
- Method: GET
- Path: /api/user/me
- Response data (UserProfileResponse): id, email, firstName, lastName, role, profileImageUrl, emailVerified, premium

### Search user emails (typeahead)
- Method: GET
- Path: /api/user/search
- Query params:
  - query: string (required) — prefix of email to search
  - limit: int (optional, default 10, max 20)
- Behavior: Returns a list of users whose email starts with the given query (case-insensitive), excludes the current user.
- Response data: List<UserEmailResponse> with fields id, email, firstName, lastName, profileImageUrl

### List user emails (for selector)
- Method: GET
- Path: /api/user/emails
- Query params:
  - limit: int (optional, default 10, max 50)
- Behavior: Returns up to N users ordered by email ascending, excluding the current user. Useful for populating a dropdown.
- Response data: List<UserEmailResponse>

Example (PowerShell):
```
$token = "<JWT>"
Invoke-RestMethod -Uri "http://localhost:8080/api/user/emails?limit=10" -Headers @{ Authorization = "Bearer $token" }
```

Example response (200 OK):
```json
{
  "success": true,
  "message": "User emails fetched",
  "data": [
    {
      "id": 2,
      "email": "alice@example.com",
      "firstName": "Alice",
      "lastName": "Smith",
      "profileImageUrl": "https://s3.amazonaws.com/bucket/profiles/user-2.jpg"
    },
    {
      "id": 3,
      "email": "bob@example.com",
      "firstName": "Bob",
      "lastName": "Lee",
      "profileImageUrl": null
    }
  ],
  "timestamp": "2025-08-16T00:00:00Z",
  "path": "/api/user/emails",
  "errors": null
}
```

Note: Both /api/user/search and /api/user/emails include profileImageUrl in each item. If the user has not uploaded a photo, profileImageUrl may be null.
