# CloudShareOriginal API Documentation (legacy)
Note: This document is superseded by API-ALL.md for the complete, up-to-date API list.

All responses are wrapped in the common envelope:

```json
{
  "success": true,
  "message": "...",
  "data": {},
  "timestamp": "2025-08-12T22:22:00Z",
  "path": "/api/...",
  "errors": {}
}
```

Authentication: JWT Bearer token is required for all endpoints except those under `/api/auth/**`.

Header for authenticated requests:
- Authorization: Bearer <JWT_TOKEN>
- Content-Type: application/json (except for file upload which uses multipart/form-data)

Base URL examples:
- Local: http://localhost:8080

---

## Auth APIs (/api/auth)

### Register
POST /api/auth/register

Request body:
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

Response data (AuthResponse):
```json
{
  "token": "<JWT>",
  "userId": 1,
  "email": "user@example.com"
}
```

cURL:
```
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!","firstName":"John","lastName":"Doe"}'
```

### Login
POST /api/auth/login

Request body:
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Response data (AuthResponse): same as above.

cURL:
```
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}'
```

### Verify Email
POST /api/auth/verify

Request body:
```json
{
  "email": "user@example.com",
  "verificationCode": "123456"
}
```

cURL:
```
curl -X POST http://localhost:8080/api/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","verificationCode":"123456"}'
```

### Forgot Password
POST /api/auth/forgot-password

Request body:
```json
{
  "email": "user@example.com"
}
```

### Reset Password
POST /api/auth/reset-password

Request body:
```json
{
  "token": "<RESET_TOKEN>",
  "newPassword": "NewPassword123!"
}
```

---

## File APIs (/api/files)

Note: All require Authorization header with a valid JWT.

### Upload a file (multipart)
POST /api/files/upload

- Content-Type: multipart/form-data
- Field: file (single file)

PowerShell example:
```
$FilePath = "C:\\path\\to\\your.pdf"
$token = "<JWT>"
Invoke-RestMethod -Uri "http://localhost:8080/api/files/upload" -Method Post \
  -Headers @{ Authorization = "Bearer $token" } \
  -Form @{ file = Get-Item $FilePath }
```

cURL example (Linux/macOS):
```
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <JWT>" \
  -F "file=@/path/to/your.pdf"
```

Response data (UploadResponse):
```json
{
  "id": 123,
  "name": "your.pdf",
  "contentType": "application/pdf",
  "size": 34567,
  "visibility": "PRIVATE",
  "fileUrl": "https://s3.../object-key"
}
```

Possible error messages related to quota:
- Free users: "Free plan limit reached. Please subscribe to upload up to 100 files." (limit is configurable)
- Subscribed users: "You have reached your subscription limit of <limit> files. Please delete some files or upgrade your plan."

### List files
GET /api/files
GET /api/files?visibility=PUBLIC|PRIVATE

cURL:
```
curl -H "Authorization: Bearer <JWT>" http://localhost:8080/api/files
```

Response: array of FileItem objects.

### Change visibility
PATCH /api/files/{id}/visibility

Request body:
```json
{ "visibility": "PUBLIC" }
```

cURL:
```
curl -X PATCH http://localhost:8080/api/files/123/visibility \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"visibility":"PUBLIC"}'
```

Response data (FileSummaryResponse) includes fileUrl if it is visible to the requester.

### Delete file
DELETE /api/files/{id}

cURL:
```
curl -X DELETE -H "Authorization: Bearer <JWT>" http://localhost:8080/api/files/123
```

### View file (metadata and accessible URL rules)
GET /api/files/{id}/view

- If PUBLIC: returns URL to anyone logged in.
- If PRIVATE: only owner sees URL, others get a safe message.

### Get download URL
GET /api/files/{id}/download-url

- Returns direct fileUrl if requester is allowed (public or owner); otherwise masked.

### Favorites
- Mark/Unmark favorite
  - PATCH /api/files/{id}/favorite?value=true|false
  - Response: FileSummaryResponse (includes favorite flag and fileUrl if accessible).
- List favorites
  - GET /api/files/favorites
  - Response: array of FileSummaryResponse (includes fileUrl for each favorite; since you own them, URLs are included).

Examples (PowerShell):
```
$token = "<JWT>"
Invoke-RestMethod -Uri "http://localhost:8080/api/files/123/favorite?value=true" -Method Patch -Headers @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/files/favorites" -Method Get -Headers @{ Authorization = "Bearer $token" }
```

### List files by user (public files for others)
GET /api/files/user/{userId}

- If you request your own ID, you get all your files; otherwise only PUBLIC files.

### Get quota
GET /api/files/quota

Response data (QuotaResponse):
```json
{ "used": 3, "limit": 5, "remaining": 2, "plan": "inactive" }
```

---

## Billing APIs (/api/billing)

All billing routes (except webhook) require Authorization: Bearer <JWT>.

### Create subscription
POST /api/billing/subscribe

What to send:
- Content-Type: application/json
- Body must include exactly one required field: planId (string)
- planId is the Razorpay Plan ID you created in your Razorpay Dashboard (e.g., plan_MnoPqRsTuVwXyZ). It is not the product name.

Request body (SubscriptionRequest):
```json
{ "planId": "plan_XXXXXXXXXXXXX" }
```

Notes:
- The user must be authenticated; the server takes your userId and email from the JWT and attaches them to the Razorpay Subscription notes/customer.
- Validation: planId cannot be blank. If missing/blank, HTTP 400 is returned by validation.
- The server creates a Razorpay Subscription with total_count = 12 (example monthly plan) and returns the subscription details.

Response data (SubscriptionResponse):
```json
{
  "subscriptionId": "sub_ABC123",
  "status": "created",
  "razorpayKey": "rzp_test_xxxxx"
}
```

How to use the response on the frontend:
- Use razorpayKey and subscriptionId with Razorpay Checkout (subscription flow). Example (JS):
```js
const options = {
  key: response.data.razorpayKey,
  subscription_id: response.data.subscriptionId,
  name: "CloudShare",
  description: "Subscription",
  handler: function (res) { /* verify and show success */ }
};
new Razorpay(options).open();
```

Examples
- cURL:
```
curl -X POST http://localhost:8080/api/billing/subscribe \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"planId":"plan_XXXXXXXXXXXXX"}'
```
- PowerShell:
```
$token = "<JWT>"
Invoke-RestMethod -Uri "http://localhost:8080/api/billing/subscribe" -Method Post \
  -Headers @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json' } \
  -Body '{"planId":"plan_XXXXXXXXXXXXX"}'
```
- Postman: set Authorization: Bearer <JWT>, body raw JSON with {"planId":"..."}.

### Webhook (Razorpay -> server)
POST /api/billing/webhook

Headers:
- X-Razorpay-Signature: <signature>

Body: Raw JSON from Razorpay subscription events.

Behavior:
- On event `subscription.activated`: activates subscription and sets file limit (default 100 via config).
- On `subscription.paused|cancelled|completed|expired`: sets subscription inactive and reduces limit to max(freeLimit, usedFiles).

Response: "ok" or 401 if signature invalid.

### Billing status (current user)
GET /api/billing/status

Returns current quota + human-friendly message about subscription status.

Sample response data (QuotaResponse inside envelope):
```json
{
  "used": 3,
  "limit": 100,
  "remaining": 97,
  "plan": "active"
}
```

---

## Notes
- Free plan file limit: `app.free.file-limit` (default 5)
- Subscription file limit: `app.subscription.file-limit` (default 100)
- Max file size per upload: `app.files.max-size-bytes` (default 10485760)
- All times are UTC ISO-8601.

## Postman
A starter Postman collection and environment exist in the `postman/` directory:
- postman/Cloudshare_Auth_API.postman_collection.json
- postman/Cloudshare_Local.postman_environment.json

Import these into Postman and set the `token` variable after login to try the protected APIs.
