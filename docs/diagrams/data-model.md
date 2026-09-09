# Kairos - Data Model Specification

## Database: PostgreSQL 16.8

This document provides a complete specification of all tables, columns, constraints, and indexes for the Kairos MVP database.

---

## Table: users

Stores user authentication and profile information.

### Columns

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| id | UUID | PK, NOT NULL | - | Unique identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | - | User email (login) |
| name | VARCHAR(100) | NOT NULL | - | Full name |
| provider | VARCHAR(20) | NOT NULL | - | OAuth provider (GOOGLE) |
| provider_id | VARCHAR(255) | NOT NULL | - | ID from OAuth provider |
| avatar_url | TEXT | NULL | - | Profile photo URL |
| active | BOOLEAN | NOT NULL | TRUE | Account active status |
| created_at | TIMESTAMP | NOT NULL | NOW() | Creation timestamp |

### Indexes

| Name | Columns | Type | Description |
|------|---------|------|-------------|
| idx_users_email | email | B-tree | Fast email lookup (login) |
| idx_users_provider | provider, provider_id | UNIQUE B-tree | OAuth identity lookup |

### Relationships

- **Has many**: organization_members (one user can be in multiple organizations)

### Business Rules

- A user can belong to multiple organizations
- Email must be unique
- Provider + provider_id combination must be unique (OAuth)
- Inactive users cannot authenticate

---

## Table: organizations

Represents multi-tenant organizations (teams/companies).

### Columns

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| id | UUID | PK, NOT NULL | - | Unique identifier |
| name | VARCHAR(100) | NOT NULL | - | Organization display name |
| slug | VARCHAR(50) | UNIQUE, NOT NULL | - | URL-friendly identifier |
| owner_id | UUID | FK → users.id, NOT NULL | - | Creator/owner |
| status | VARCHAR(20) | NOT NULL, CHECK | 'ACTIVE' | ACTIVE or ARCHIVED |
| created_at | TIMESTAMP | NOT NULL | NOW() | Creation timestamp |
| deleted_at | TIMESTAMP | NULL | - | Soft delete timestamp |

### Check Constraints

```sql
CHECK (status IN ('ACTIVE', 'ARCHIVED'))
```

### Indexes

| Name | Columns | Type | Filter | Description |
|------|---------|------|--------|-------------|
| idx_org_slug | slug | UNIQUE | - | Slug lookup (URLs) |
| idx_org_owner | owner_id | B-tree | - | Owner's organizations |
| idx_org_status | status | B-tree | deleted_at IS NULL | Active orgs filter |
| idx_org_deleted | deleted_at | B-tree | deleted_at IS NOT NULL | Cleanup queries |

### Relationships

- **Belongs to**: users (via owner_id)
- **Has many**: organization_members, projects, tasks, time_blocks, work_sessions

### Business Rules

- Slug must be unique and URL-safe
- Soft delete: deleted_at = NULL means active
- Owner is automatically a member with OWNER role

---

## Table: organization_members

Junction table for user-organization memberships with roles.

### Columns

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| id | UUID | PK, NOT NULL | - | Unique identifier |
| organization_id | UUID | FK → organizations.id, NOT NULL | - | Organization |
| user_id | UUID | FK → users.id, NOT NULL | - | User |
| role | VARCHAR(20) | NOT NULL, CHECK | 'MEMBER' | OWNER, ADMIN, MEMBER |
| joined_at | TIMESTAMP | NOT NULL | NOW() | Join timestamp |

### Check Constraints

```sql
CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
```

### Unique Constraints

```sql
UNIQUE (organization_id, user_id)
```

**Rationale**: A user can only be a member once per organization.

### Indexes

| Name | Columns | Type | Description |
|------|---------|------|-------------|
| uq_org_user | organization_id, user_id | UNIQUE | Prevent duplicate memberships |
| idx_members_org | organization_id | B-tree | List members by org |
| idx_members_user | user_id | B-tree | List user's memberships |

### Relationships

- **Belongs to**: organizations, users

### Business Rules

| Role | Permissions |
|------|-------------|
| OWNER | Full control, can delete organization |
| ADMIN | Manage projects, members, cannot delete org |
| MEMBER | Read/write own content, cannot manage members |

---

## Table: projects

Represents long-term work contexts (study areas, clients, projects).

### Columns

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| id | UUID | PK, NOT NULL | - | Unique identifier |
| organization_id | UUID | FK → organizations.id, NOT NULL | - | Owner organization |
| name | VARCHAR(100) | NOT NULL | - | Project name |
| description | TEXT | NULL | - | Detailed description |
| color | VARCHAR(7) | NULL | - | Hex color (#RRGGBB) |
| status | VARCHAR(20) | NOT NULL, CHECK | 'ACTIVE' | ACTIVE or ARCHIVED |
| created_at | TIMESTAMP | NOT NULL | NOW() | Creation timestamp |
| deleted_at | TIMESTAMP | NULL | - | Soft delete timestamp |

### Check Constraints

```sql
CHECK (status IN ('ACTIVE', 'ARCHIVED'))
```

### Indexes

| Name | Columns | Type | Filter | Description |
|------|---------|------|--------|-------------|
| idx_projects_org | organization_id | B-tree | - | Projects by organization |
| idx_projects_status | status | B-tree | deleted_at IS NULL | Active projects |
| idx_projects_deleted | deleted_at | B-tree | deleted_at IS NOT NULL | Cleanup queries |
| idx_projects_org_status | organization_id, status, deleted_at | B-tree | - | Common filter combo |

### Relationships

- **Belongs to**: organizations
- **Has many**: tasks, time_blocks (optional)

### Business Rules

- Projects belong to exactly one organization
- Archived projects are read-only
- Soft deleted projects are hidden from normal queries
- Color format must be #RRGGBB hex

---

## Table: tasks

Represents individual work units in the Kanban system.

### Columns

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| id | UUID | PK, NOT NULL | - | Unique identifier |
| organization_id | UUID | FK → organizations.id, NOT NULL | - | Owner organization |
| title | VARCHAR(200) | NOT NULL | - | Task title |
| description | TEXT | NULL | - | Detailed description |
| status | VARCHAR(20) | NOT NULL, CHECK | 'TODO' | TODO, DOING, DONE |
| project_id | UUID | FK → projects.id, NOT NULL | - | Parent project |
| time_block_id | UUID | FK → time_blocks.id, NULL | - | Optional scheduling |
| created_at | TIMESTAMP | NOT NULL | NOW() | Creation timestamp |
| deleted_at | TIMESTAMP | NULL | - | Soft delete timestamp |

### Check Constraints

```sql
CHECK (status IN ('TODO', 'DOING', 'DONE'))
```

### Indexes

| Name | Columns | Type | Filter | Description |
|------|---------|------|--------|-------------|
| idx_tasks_org | organization_id | B-tree | - | Tasks by organization |
| idx_tasks_project | project_id | B-tree | - | Tasks by project |
| idx_tasks_status | status | B-tree | deleted_at IS NULL | Kanban filter |
| idx_tasks_deleted | deleted_at | B-tree | deleted_at IS NOT NULL | Cleanup queries |
| idx_tasks_org_status | organization_id, status, deleted_at | B-tree | - | Kanban by org |
| idx_tasks_timeblock | time_block_id | B-tree | time_block_id IS NOT NULL | Scheduled tasks |

### Relationships

- **Belongs to**: organizations, projects
- **Optionally references**: time_blocks
- **Has many**: work_sessions (optional)

### Business Rules

- **CRITICAL**: Task can only be marked as `DONE` if at least one work_session exists with task_id
- Tasks must belong to a project (not optional)
- Tasks can exist without time_block (unscheduled work)
- Status flow: TODO → DOING → DONE (can move backwards)

---

## Table: time_blocks

Represents scheduled time blocks in the calendar.

### Columns

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| id | UUID | PK, NOT NULL | - | Unique identifier |
| organization_id | UUID | FK → organizations.id, NOT NULL | - | Owner organization |
| title | VARCHAR(200) | NOT NULL | - | Block title |
| start_datetime | TIMESTAMP | NOT NULL | - | Start time |
| end_datetime | TIMESTAMP | NOT NULL | - | End time |
| project_id | UUID | FK → projects.id, NULL | - | Optional project |
| created_at | TIMESTAMP | NOT NULL | NOW() | Creation timestamp |
| deleted_at | TIMESTAMP | NULL | - | Soft delete timestamp |

### Check Constraints

```sql
CHECK (end_datetime > start_datetime)
```

**Rationale**: Prevent invalid time ranges.

### Indexes

| Name | Columns | Type | Filter | Description |
|------|---------|------|--------|-------------|
| idx_timeblocks_org | organization_id | B-tree | - | Blocks by organization |
| idx_timeblocks_range | start_datetime, end_datetime | B-tree | deleted_at IS NULL | Calendar view queries |
| idx_timeblocks_deleted | deleted_at | B-tree | deleted_at IS NOT NULL | Cleanup queries |
| idx_timeblocks_org_date | organization_id, start_datetime | B-tree | deleted_at IS NULL | Calendar by org |

### Relationships

- **Belongs to**: organizations
- **Optionally belongs to**: projects
- **Has many**: tasks, work_sessions

### Business Rules

- Time blocks are **intentions**, not execution (no metrics generated)
- Can exist without project (general time blocking)
- Multiple tasks can be scheduled in same time block
- Can "pass unused" (no execution logged)
- Soft deleted for calendar history

---

## Table: work_sessions

Represents actual work execution (source of truth for metrics).

### Columns

| Column | Type | Constraints | Default | Description |
|--------|------|-------------|---------|-------------|
| id | UUID | PK, NOT NULL | - | Unique identifier |
| organization_id | UUID | FK → organizations.id, NOT NULL | - | Owner organization |
| duration_minutes | INTEGER | NOT NULL, CHECK | - | Duration in minutes |
| date | DATE | NOT NULL | - | Work date |
| task_id | UUID | FK → tasks.id, NULL | - | Optional task |
| time_block_id | UUID | FK → time_blocks.id, NULL | - | Optional time block |
| notes | TEXT | NULL | - | Work notes |
| created_at | TIMESTAMP | NOT NULL | NOW() | Creation timestamp |

### Check Constraints

```sql
CHECK (duration_minutes >= 1)
```

**Rationale**: Prevent zero or negative duration.

### Indexes

| Name | Columns | Type | Filter | Description |
|------|---------|------|--------|-------------|
| idx_sessions_org | organization_id | B-tree | - | Sessions by organization |
| idx_sessions_date | session_date | B-tree | - | Daily/weekly reports |
| idx_logs_task | task_id | B-tree | task_id IS NOT NULL | Task completion check |
| idx_logs_timeblock | time_block_id | B-tree | time_block_id IS NOT NULL | Time block analysis |
| idx_sessions_org_date | organization_id, session_date | B-tree | - | Metrics by org/date |

### Relationships

- **Belongs to**: organizations
- **Optionally references**: tasks, time_blocks

### Business Rules

- **Physical deletion** (no soft delete) for performance and volume
- At least one of task_id or time_block_id should be set (validation in app)
- Only execution logs generate productivity metrics
- duration_minutes must be >= 1

---

## Cascade Operations

### Soft Delete Cascade

When a Project is soft deleted (`deleted_at` is set):

1. **Application level** (not database trigger):
   ```sql
   UPDATE tasks
   SET deleted_at = NOW()
   WHERE project_id = ? AND deleted_at IS NULL;
   ```

2. **Time blocks** are NOT cascade deleted (they can exist independently)

### Hard Delete Cascade

Execution logs use physical deletion. When deleted:

- No cascade (logs are standalone records)
- Task completion status must be revalidated

---

## Performance Considerations

### Partial Indexes

PostgreSQL partial indexes filter out deleted rows:

```sql
-- Only index active tasks
CREATE INDEX idx_tasks_active
ON tasks(organization_id, status)
WHERE deleted_at IS NULL;

-- Faster calendar queries
CREATE INDEX idx_timeblocks_calendar
ON time_blocks(start_datetime, end_datetime)
WHERE deleted_at IS NULL;
```

**Benefits**:
- Smaller index size
- Faster query performance
- Automatic exclusion of deleted records

### Common Query Patterns

#### 1. Kanban Board (Tasks by Status)
```sql
SELECT * FROM tasks
WHERE organization_id = ?
  AND status = 'DOING'
  AND deleted_at IS NULL
ORDER BY created_at DESC;

-- Uses: idx_tasks_org_status
```

#### 2. Calendar View (Time Blocks by Date Range)
```sql
SELECT * FROM time_blocks
WHERE organization_id = ?
  AND start_datetime >= ?
  AND end_datetime < ?
  AND deleted_at IS NULL
ORDER BY start_datetime;

-- Uses: idx_timeblocks_range
```

#### 3. Task Completion Validation
```sql
SELECT COUNT(*) FROM work_sessions
WHERE task_id = ?;

-- Uses: idx_sessions_task
```

#### 4. Daily Metrics
```sql
SELECT date, SUM(duration_minutes) as total
FROM work_sessions
WHERE organization_id = ?
  AND date BETWEEN ? AND ?
GROUP BY date
ORDER BY date;

-- Uses: idx_logs_org_date
```

---

## Data Migration Strategy

### Flyway Versioning

| Version | File | Description |
|---------|------|-------------|
| V1 | V1__Create_users.sql | Users table |
| V2 | V2__Create_organizations.sql | Organizations table |
| V3 | V3__Create_organization_members.sql | Organization members |
| V4 | V4__Create_projects.sql | Projects table |
| V5 | V5__Create_tasks.sql | Tasks table |
| V6 | V6__Create_time_blocks.sql | Time blocks table |
| V7 | V7__Create_work_sessions.sql | Work sessions table |
| V8 | V8__Create_performance_indexes.sql | Optimized indexes |

### Rollback Strategy

Flyway migrations should be designed to be reversible:

```sql
-- V1__Create_users.sql
CREATE TABLE users (...);

-- V1__Undo__Drop_users.sql (for rollback)
DROP TABLE users;
```

---

## Security Considerations

### Row-Level Security (Optional - Future)

PostgreSQL RLS policies can enforce organization isolation:

```sql
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;

CREATE POLICY projects_org_policy ON projects
FOR ALL
USING (organization_id IN (
    SELECT organization_id FROM organization_members
    WHERE user_id = current_user_id()
));
```

**Note**: For MVP, organization membership is validated at application level.

### Sensitive Data

| Field | Sensitivity | Protection |
|-------|-------------|------------|
| users.email | PII | Encrypted at rest, access logs |
| users.name | PII | Encrypted at rest |
| users.provider_id | PII | Internal only, never exposed |
| work_sessions.notes | User data | No special protection |

---

## Database Capacity Planning

### Estimated Growth (1 year, 100 users)

| Entity | Records | Storage | Index Size |
|--------|---------|---------|------------|
| users | 100 | 16 KB | 8 KB |
| organizations | 50 | 8 KB | 4 KB |
| projects | 500 | 80 KB | 40 KB |
| tasks | 5,000 | 800 KB | 400 KB |
| time_blocks | 10,000 | 1.6 MB | 800 KB |
| work_sessions | 50,000 | 8 MB | 4 MB |

**Total (1 year)**: ~15 MB + indexes

**Cleanup Policy**:
- Execution logs: Physical delete after 2 years (configurable)
- Soft deleted entities: Physical delete after 6 months (configurable)

---

## Connection Pooling (HikariCP)

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**Rationale**:
- 10 connections sufficient for 100 concurrent users
- Connection timeout: 30 seconds
- Max lifetime: 30 minutes (avoid stale connections)
