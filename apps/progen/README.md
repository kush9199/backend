# PRD: Trello Clone (Board/List/Card App)

## 1. Problem

Team need visual task track. Spreadsheet bad — no drag, no status-see-quick. Need kanban board: card move column, see work state glance.

## 2. Goal

- User make board, organize work in list+card, move card drag-drop.
- Team collab: assign, comment, due date.
- Metric: user create ≥1 board in first session. Card move latency <200ms.

## 3. User

- Small team (3-15 people): PM, dev, designer.
- Personal task user (solo todo-style use).

## 4. Core Features (MVP)

1. **Workspace** — group of boards, has member.
2. **Board** — belong workspace, has list.
3. **List** (column) — belong board, ordered, has card.
4. **Card** — belong list, ordered, has title/desc/label/due/assignee/comment/attachment.
5. **Drag-drop** — card move list-to-list, reorder within list.
6. **Member + role** — owner/admin/member per workspace.
7. **Label** — color tag on card, board-scope.
8. **Comment** — card discussion thread.
9. **Activity log** — who-did-what audit trail.
10. **Auth** — signup/login, invite by email.

## 5. Not-Build (v1 scope wall)

- No calendar/timeline view (Gantt).
- No automation/butler-rule engine.
- No power-up/plugin marketplace.
- No offline mode.

## 6. Success Metric

- Board creation → first card < 60s (onboarding speed).
- Weekly active board ratio > 40%.
- API p95 latency < 300ms.

---

# 2. Data Model (core entities)

```
Workspace(id, name, owner_id, created_at)
Board(id, workspace_id, name, background, created_at)
List(id, board_id, name, position, created_at)
Card(id, list_id, title, description, position, due_date, created_at)
Member(id, email, name, password_hash, created_at)
BoardMember(board_id, user_id, role)
Label(id, board_id, name, color)
CardLabel(card_id, label_id)
CardAssignee(card_id, user_id)
Comment(id, card_id, user_id, body, created_at)
Attachment(id, card_id, url, filename, uploaded_by, created_at)
Activity(id, board_id, user_id, action, entity_type, entity_id, created_at)
```

`position` = float (fractional indexing) — reorder without reindex whole list.

---

# 3. API Spec (REST, JSON, v1)

Base URL: `/api/v1`
Auth: Bearer JWT in `Authorization` header, all routes except `/auth/*` need it.

## 3.1 Auth

**POST /auth/signup**

```json
Request:  { "email": "a@x.com", "name": "Kush", "password": "***" }
Response: 201 { "id": "u1", "email": "a@x.com", "name": "Kush", "token": "jwt..." }
```

**POST /auth/login**

```json
Request:  { "email": "a@x.com", "password": "***" }
Response: 200 { "token": "jwt...", "user": { "id": "u1", "name": "Kush" } }
Errors:   401 { "error": "invalid_credentials" }
```

## 3.2 Workspace

**POST /workspaces**

```json
Request:  { "name": "Nagarro Team" }
Response: 201 { "id": "w1", "name": "Nagarro Team", "owner_id": "u1" }
```

**GET /workspaces** → list workspace user belong to

```json
Response: 200 [ { "id": "w1", "name": "Nagarro Team" } ]
```

**GET /workspaces/{id}** → single workspace detail + member list

## 3.3 Board

**POST /workspaces/{workspaceId}/boards**

```json
Request:  { "name": "Sprint 12", "background": "#0079BF" }
Response: 201 { "id": "b1", "workspace_id": "w1", "name": "Sprint 12" }
```

**GET /boards/{id}** → full board: lists + cards nested

```json
Response: 200 {
  "id": "b1", "name": "Sprint 12",
  "lists": [
    { "id": "l1", "name": "To Do", "position": 1000,
      "cards": [ { "id": "c1", "title": "Fix login bug", "position": 1000 } ] }
  ]
}
```

**PATCH /boards/{id}** — update name/background
**DELETE /boards/{id}**

## 3.4 List

**POST /boards/{boardId}/lists**

```json
Request:  { "name": "In Progress", "position": 2000 }
Response: 201 { "id": "l2", "board_id": "b1", "name": "In Progress", "position": 2000 }
```

**PATCH /lists/{id}** — rename or reposition

```json
Request: { "position": 1500 }
```

**DELETE /lists/{id}**

## 3.5 Card

**POST /lists/{listId}/cards**

```json
Request:  { "title": "Fix login bug", "position": 1000 }
Response: 201 { "id": "c1", "list_id": "l1", "title": "Fix login bug" }
```

**GET /cards/{id}** → full card detail (label, assignee, comment, attachment)

**PATCH /cards/{id}** — update field OR move card

```json
Request: { "list_id": "l2", "position": 1500 }
```

Note: `list_id` change + `position` in one call = drag-drop move. Position recompute client-side (avg of neighbor) or server rebalance if collision.

**DELETE /cards/{id}**

## 3.6 Label

**POST /boards/{boardId}/labels**

```json
Request: { "name": "Bug", "color": "red" }
```

**POST /cards/{cardId}/labels/{labelId}** — attach
**DELETE /cards/{cardId}/labels/{labelId}** — detach

## 3.7 Assignee

**POST /cards/{cardId}/assignees**

```json
Request: { "user_id": "u2" }
```

**DELETE /cards/{cardId}/assignees/{userId}**

## 3.8 Comment

**POST /cards/{cardId}/comments**

```json
Request:  { "body": "LGTM, merging" }
Response: 201 { "id": "cm1", "user_id": "u1", "body": "LGTM, merging", "created_at": "..." }
```

**GET /cards/{cardId}/comments** — paginated, `?page=1&limit=20`

## 3.9 Member Invite

**POST /boards/{boardId}/members**

```json
Request: { "email": "new@x.com", "role": "member" }
```

**PATCH /boards/{boardId}/members/{userId}** — change role
**DELETE /boards/{boardId}/members/{userId}** — remove

## 3.10 Activity Feed

**GET /boards/{boardId}/activity** → paginated log

```json
Response: 200 [
  { "user": "Kush", "action": "moved_card", "detail": "Fix login bug → In Progress", "at": "..." }
]
```

---

# 4. Real-time (websocket, optional Phase 2)

`ws://.../boards/{boardId}/live`
Event push on: card_moved, card_created, comment_added, member_joined.

```json
{ "event": "card_moved", "card_id": "c1", "from_list": "l1", "to_list": "l2", "position": 1500 }
```

---

# 5. Error Format (standard across API)

```json
{ "error": "not_found", "message": "Card c99 not found", "status": 404 }
```

Codes: 400 validation, 401 unauth, 403 forbid, 404 not-found, 409 conflict (position collision), 500 server.

---

# 6. Tech Stack Suggestion

- Backend: Spring Boot / Node+Express — REST above stack-agnostic.
- DB: PostgreSQL (relational fit — FK heavy).
- Real-time: WebSocket or Kafka event → push.
- Auth: JWT + refresh token.
- Frontend: React + dnd-kit (drag-drop lib).
