# Kairos - Query Patterns & Performance

This document outlines common query patterns, their optimizations, and performance considerations for the Kairos MVP database.

---

## Table of Contents

1. [Core Queries](#core-queries)
2. [Kanban Queries](#kanban-queries)
3. [Calendar Queries](#calendar-queries)
4. [Metrics Queries](#metrics-queries)
5. [Search Queries](#search-queries)
6. [Performance Optimization](#performance-optimization)
7. [N+1 Query Prevention](#n1-query-prevention)

---

## Core Queries

### Query: Get Organization Details

```sql
SELECT o.id, o.name, o.slug, o.status, o.created_at,
       u.id as owner_id, u.name as owner_name, u.email as owner_email
FROM organizations o
JOIN users u ON o.owner_id = u.id
WHERE o.id = ? AND o.deleted_at IS NULL;
```

**Index Used**: Primary key lookup

**Performance**: O(1)

**Use Case**: Organization dashboard, settings page

---

### Query: Check Organization Membership

```sql
SELECT 1 FROM organization_members
WHERE organization_id = ? AND user_id = ?;
```

**Index Used**: `uq_org_user` (organization_id, user_id)

**Performance**: O(log n)

**Use Case**: Authorization check before every API call

**Application Validation**:
```java
if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, userId)) {
    throw new AccessDeniedException("Not a member");
}
```

---

### Query: List User's Organizations

```sql
SELECT o.id, o.name, o.slug, om.role, om.joined_at
FROM organizations o
JOIN organization_members om ON o.id = om.organization_id
WHERE om.user_id = ? AND o.deleted_at IS NULL
ORDER BY o.name;
```

**Index Used**: `idx_members_user`

**Performance**: O(n) where n = user's organizations

**Use Case**: Organization switcher in UI

---

## Kanban Queries

### Query: Get Tasks by Status (Kanban Column)

```sql
SELECT t.id, t.title, t.status, t.project_id, p.name as project_name,
       p.color as project_color, t.created_at
FROM tasks t
JOIN projects p ON t.project_id = p.id
WHERE t.organization_id = ?
  AND t.status = ?
  AND t.deleted_at IS NULL
  AND p.deleted_at IS NULL
ORDER BY t.created_at DESC;
```

**Index Used**: `idx_tasks_org_status`

**Performance**: O(log n + m) where m = tasks with given status

**Parameters**: `organization_id`, `status` ('TODO', 'DOING', 'DONE')

**Use Case**: Kanban board rendering

**Response Time**: < 50ms for 1000 tasks

---

### Query: Get All Tasks for Organization (Kanban Board)

```sql
-- Single query to fetch all columns
SELECT t.id, t.title, t.description, t.status,
       t.project_id, p.name as project_name, p.color as project_color,
       t.time_block_id, tb.start_datetime, tb.end_datetime,
       COUNT(el.id) as work_session_count
FROM tasks t
JOIN projects p ON t.project_id = p.id
LEFT JOIN time_blocks tb ON t.time_block_id = tb.id AND tb.deleted_at IS NULL
LEFT JOIN work_sessions el ON t.id = el.task_id
WHERE t.organization_id = ?
  AND t.deleted_at IS NULL
  AND p.deleted_at IS NULL
GROUP BY t.id, p.id, tb.id
ORDER BY t.status, t.created_at DESC;
```

**Index Used**: `idx_tasks_org_status`

**Performance**: O(n) where n = tasks in organization

**Use Case**: Load entire Kanban board in single query

**Optimization**: Pre-aggregate execution log count in tasks table (future)

---

### Query: Validate Task Completion

```sql
-- Check if task has execution logs before marking as DONE
SELECT COUNT(*) as log_count
FROM work_sessions
WHERE task_id = ?;
```

**Index Used**: `idx_logs_task`

**Performance**: O(log n) where n = logs for this task

**Application Logic**:
```java
if (logCount == 0) {
    throw new TaskCompletionException(
        "Task can only be marked as DONE if execution time is logged"
    );
}
```

---

### Query: Task with Time Block and Logs

```sql
SELECT t.id, t.title, t.description, t.status,
       t.project_id, p.name as project_name, p.color,
       tb.id as time_block_id, tb.title as time_block_title,
       tb.start_datetime, tb.end_datetime,
       json_agg(json_build_object(
           'id', el.id,
           'duration_minutes', el.duration_minutes,
           'date', el.date,
           'notes', el.notes
       )) as work_sessions
FROM tasks t
JOIN projects p ON t.project_id = p.id
LEFT JOIN time_blocks tb ON t.time_block_id = tb.id
LEFT JOIN work_sessions el ON t.id = el.task_id
WHERE t.id = ? AND t.deleted_at IS NULL
GROUP BY t.id, p.id, tb.id;
```

**Use Case**: Task detail view

**Performance**: O(1) for single task

---

## Calendar Queries

### Query: Get Time Blocks by Date Range (Day View)

```sql
SELECT tb.id, tb.title, tb.start_datetime, tb.end_datetime,
       tb.project_id, p.name as project_name, p.color,
       COUNT(t.id) as task_count
FROM time_blocks tb
LEFT JOIN projects p ON tb.project_id = p.id AND p.deleted_at IS NULL
LEFT JOIN tasks t ON tb.id = t.time_block_id AND t.deleted_at IS NULL
WHERE tb.organization_id = ?
  AND tb.start_datetime >= ?
  AND tb.end_datetime < ?
  AND tb.deleted_at IS NULL
GROUP BY tb.id, p.id
ORDER BY tb.start_datetime;
```

**Index Used**: `idx_timeblocks_range`

**Parameters**:
- `organization_id`
- `start_datetime` (beginning of day)
- `end_datetime` (end of day or end of week)

**Performance**: O(log n + m) where m = blocks in range

**Use Case**: Day/week/month calendar view

**Response Time**: < 100ms for 1000 time blocks in range

---

### Query: Get Time Blocks for Multiple Tasks

```sql
SELECT DISTINCT tb.id, tb.title, tb.start_datetime, tb.end_datetime,
       tb.project_id, p.name as project_name, p.color
FROM time_blocks tb
JOIN tasks t ON tb.id = t.time_block_id
JOIN projects p ON tb.project_id = p.id
WHERE t.id IN (?, ?, ?)
  AND t.organization_id = ?
  AND t.deleted_at IS NULL
  AND tb.deleted_at IS NULL
ORDER BY tb.start_datetime;
```

**Index Used**: `idx_tasks_timeblock`

**Use Case**: Show scheduled time when viewing tasks

---

## Metrics Queries

### Query: Daily Execution Summary (Last 7 Days)

```sql
SELECT date,
       SUM(duration_minutes) as total_minutes,
       COUNT(*) as log_count,
       COUNT(DISTINCT task_id) as tasks_worked,
       COUNT(DISTINCT time_block_id) as time_blocks_worked
FROM work_sessions
WHERE organization_id = ?
  AND date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY date
ORDER BY date;
```

**Index Used**: `idx_logs_org_date`

**Performance**: O(n) where n = logs in date range

**Response Format**:
```json
[
  {
    "date": "2025-01-15",
    "total_minutes": 480,
    "log_count": 12,
    "tasks_worked": 5,
    "time_blocks_worked": 3
  },
  ...
]
```

**Use Case**: Daily/weekly productivity dashboard

---

### Query: Project Execution Metrics

```sql
SELECT p.id, p.name, p.color,
       COALESCE(SUM(el.duration_minutes), 0) as total_minutes,
       COUNT(el.id) as log_count,
       COUNT(DISTINCT el.date) as days_worked
FROM projects p
LEFT JOIN tasks t ON p.id = t.project_id AND t.deleted_at IS NULL
LEFT JOIN work_sessions el ON t.id = el.task_id
WHERE p.organization_id = ?
  AND p.deleted_at IS NULL
  AND (el.date >= ? OR el.date IS NULL)
GROUP BY p.id
ORDER BY total_minutes DESC;
```

**Index Used**: `idx_logs_org_date`, `idx_tasks_project`

**Use Case**: Project progress report

**Caching**: Redis cache with 5-minute TTL

```java
@Cacheable(value = "projectMetrics", key = "#organizationId + ':' + #startDate")
public List<ProjectMetrics> getProjectMetrics(UUID organizationId, LocalDate startDate) {
    // query execution
}
```

---

### Query: Task Completion Rate

```sql
SELECT
    COUNT(*) FILTER (WHERE status = 'DONE') as done_count,
    COUNT(*) FILTER (WHERE status = 'DOING') as doing_count,
    COUNT(*) FILTER (WHERE status = 'TODO') as todo_count,
    COUNT(*) as total_count,
    ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'DONE') / COUNT(*), 2) as completion_rate
FROM tasks
WHERE organization_id = ?
  AND deleted_at IS NULL;
```

**Index Used**: `idx_tasks_org_status`

**Use Case**: Productivity insights

**Response Format**:
```json
{
  "done_count": 15,
  "doing_count": 5,
  "todo_count": 10,
  "total_count": 30,
  "completion_rate": 50.00
}
```

---

### Query: Consistency Streak (Active Days)

```sql
WITH date_series AS (
    SELECT generate_series(
        CURRENT_DATE - INTERVAL '30 days',
        CURRENT_DATE,
        INTERVAL '1 day'
    )::date as work_date
),
daily_logs AS (
    SELECT date, COUNT(*) as log_count
    FROM work_sessions
    WHERE organization_id = ?
      AND date >= CURRENT_DATE - INTERVAL '30 days'
    GROUP BY date
)
SELECT
    COUNT(*) as active_days,
    MAX(CASE WHEN dl.log_count > 0 THEN 1 ELSE 0 END) as current_streak
FROM date_series ds
LEFT JOIN daily_logs dl ON ds.work_date = dl.date;
```

**Use Case**: Gamification, consistency tracking

---

## Search Queries

### Query: Full-Text Search (Tasks)

```sql
-- Basic ILIKE search (MVP)
SELECT t.id, t.title, t.status, p.name as project_name
FROM tasks t
JOIN projects p ON t.project_id = p.id
WHERE t.organization_id = ?
  AND t.deleted_at IS NULL
  AND (t.title ILIKE '%' || ? || '%'
       OR t.description ILIKE '%' || ? || '%')
ORDER BY t.created_at DESC
LIMIT 20;
```

**Future Enhancement**: PostgreSQL Full-Text Search
```sql
ALTER TABLE tasks ADD COLUMN search_vector tsvector;
CREATE INDEX idx_tasks_search ON tasks USING gin(search_vector);

UPDATE tasks SET search_vector = to_tsvector('english',
    coalesce(title, '') || ' ' || coalesce(description, ''));

SELECT * FROM tasks
WHERE search_vector @@ to_tsquery('english', 'keyword');
```

---

### Query: Filter Tasks by Multiple Criteria

```sql
SELECT t.id, t.title, t.status, t.project_id, p.name as project_name
FROM tasks t
JOIN projects p ON t.project_id = p.id
WHERE t.organization_id = ?
  AND t.deleted_at IS NULL
  AND (? IS NULL OR t.status = ?)           -- status filter
  AND (? IS NULL OR t.project_id = ?)       -- project filter
  AND (? IS NULL OR t.time_block_id IS NOT NULL) -- scheduled filter
ORDER BY t.created_at DESC;
```

**Use Case**: Advanced task filtering in Kanban

**Performance**: O(n) with index on organization_id

---

## Performance Optimization

### EXPLAIN ANALYZE Examples

#### Before Optimization (No Index)

```sql
EXPLAIN ANALYZE
SELECT * FROM tasks
WHERE organization_id = ? AND status = 'DOING';

-- Result: Seq Scan on tasks (cost=0.00..150.00 rows=50 width=500)
--         Actual time: 45.234..123.456 rows=50
```

#### After Optimization (With Index)

```sql
EXPLAIN ANALYZE
SELECT * FROM tasks
WHERE organization_id = ? AND status = 'DOING';

-- Result: Index Scan using idx_tasks_org_status (cost=0.42..8.45 rows=50)
--         Actual time: 0.123..0.234 rows=50
```

**Improvement**: ~500x faster

---

### Index Optimization Tips

1. **Partial Indexes for Active Records**
   ```sql
   CREATE INDEX idx_tasks_active
   ON tasks(organization_id, status)
   WHERE deleted_at IS NULL;
   ```

2. **Composite Indexes for Common Filters**
   ```sql
   CREATE INDEX idx_tasks_filters
   ON tasks(organization_id, status, project_id)
   WHERE deleted_at IS NULL;
   ```

3. **Covering Indexes (INCLUDE)**
   ```sql
   CREATE INDEX idx_tasks_covering
   ON tasks(organization_id, status)
   INCLUDE (title, created_at);
   ```

---

## N+1 Query Prevention

### Problem: N+1 Query Pattern

```java
// BAD: N+1 queries
List<Task> tasks = taskRepository.findByOrganizationId(orgId);
for (Task task : tasks) {
    List<WorkSession> logs = workSessionRepository.findByTaskId(task.getId()); // N queries
    task.setLogs(logs);
}
```

**Total Queries**: 1 + N (where N = tasks)

---

### Solution: Single Query with JOIN

```sql
-- GOOD: Single query fetches all data
SELECT t.id, t.title, t.status,
       json_agg(json_build_object(
           'id', el.id,
           'duration_minutes', el.duration_minutes,
           'date', el.date
       )) as work_sessions
FROM tasks t
LEFT JOIN work_sessions el ON t.id = el.task_id
WHERE t.organization_id = ?
GROUP BY t.id;
```

**Total Queries**: 1

---

### Solution: Batch Fetching (JPA)

```java
// GOOD: EntityGraph for batch loading
@EntityGraph(attributePaths = {"executionLogs"})
List<Task> findByOrganizationId(UUID organizationId);

// Or use JOIN FETCH
@Query("SELECT t FROM Task t LEFT JOIN FETCH t.executionLogs WHERE t.organizationId = :orgId")
List<Task> findAllWithLogs(@Param("orgId") UUID organizationId);
```

---

## Connection Pool Monitoring

### HikariCP Metrics

```properties
# Actuator endpoint
management.metrics.export.prometheus.enabled=true
management.endpoint.health.show-details=always
```

**Metrics to Monitor**:
- `hikaricp.connections.active` - Current active connections
- `hikaricp.connections.idle` - Idle connections
- `hikaricp.connections.max` - Max pool size
- `hikaricp.connections.min` - Min pool size

**Alerting**:
- Active connections > 80% of max for > 5 minutes
- Connection timeout rate > 1%

---

## Query Performance Benchmarks

### Target Response Times (p95)

| Query Type | Target | Acceptable |
|------------|--------|------------|
| Single record lookup | < 10ms | < 50ms |
| List (100 records) | < 50ms | < 200ms |
| List (1000 records) | < 200ms | < 500ms |
| Aggregations (metrics) | < 100ms | < 500ms |
| Full-text search | < 200ms | < 1000ms |

### Load Testing (k6)

```javascript
// load-test.js
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  vus: 100,
  duration: '30s',
};

export default function () {
  let response = http.get('http://localhost:8080/api/tasks?organizationId=xxx');
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });
}
```

**Run**:
```bash
k6 run --vus 100 --duration 30s load-test.js
```

---

## Query Caching Strategy

### Redis Cache Keys

```
# Organization metrics
org:{organizationId}:metrics:{date}

# Project list
org:{organizationId}:projects

# Kanban board
org:{organizationId}:kanban:{status}

# Daily summary
org:{organizationId}:daily:{startDate}:{endDate}
```

### Cache TTL

| Data | TTL | Reason |
|------|-----|--------|
| Metrics (aggregated) | 5 minutes | Updates frequently |
| Project list | 30 minutes | Rarely changes |
| Kanban board | 1 minute | Updates frequently |
| User profile | 1 hour | Rarely changes |

### Cache Invalidation

```java
@CacheEvict(value = "projectMetrics", key = "#organizationId")
public void updateProject(UUID organizationId, UUID projectId, UpdateProjectRequest request) {
    // update logic
}
```

---

## Slow Query Log

### PostgreSQL Configuration

```sql
-- Log queries slower than 100ms
ALTER DATABASE kairos_db SET log_min_duration_statement = 100;

-- Check slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

**Action Items**:
1. Identify queries > 100ms
2. Run EXPLAIN ANALYZE
3. Add or optimize indexes
4. Rewrite query if needed

---

This document should be updated as the schema evolves and query patterns change.
