create table if not exists scheduled_task
(
    id              uuid primary key,
    task_type       varchar(100)             not null,
    cron_expression varchar(120)             not null,
    payload         text,
    status          varchar(30)              not null,
    next_run_at     timestamp with time zone not null,
    attempt         integer                  not null default 0,
    max_attempts    integer                  not null default 3,
    lock_owner      varchar(120),
    lock_until      timestamp with time zone,
    last_started_at timestamp with time zone,
    last_finished_at timestamp with time zone,
    created_at      timestamp with time zone not null default now(),
    updated_at      timestamp with time zone not null default now()
);

create index if not exists idx_scheduled_task_due
    on scheduled_task (status, next_run_at);

create index if not exists idx_scheduled_task_lock_until
    on scheduled_task (lock_until);

create table if not exists scheduled_task_error
(
    id            uuid primary key,
    task_id       uuid                     not null references scheduled_task (id),
    attempt       integer                  not null,
    error_type    varchar(255)             not null,
    error_message text,
    stack_trace   text,
    created_at    timestamp with time zone not null default now()
);

create index if not exists idx_scheduled_task_error_task_id
    on scheduled_task_error (task_id, created_at desc);
