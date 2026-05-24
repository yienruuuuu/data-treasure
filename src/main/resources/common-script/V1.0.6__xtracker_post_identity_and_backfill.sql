alter table xtracker_crawled_post
    add column if not exists tracker_post_id varchar(128),
    add column if not exists platform_post_id varchar(128),
    add column if not exists imported_at timestamp with time zone;

update xtracker_crawled_post post
set tracker_post_id = item.value ->> 'id',
    platform_post_id = item.value ->> 'platformId',
    imported_at = nullif(item.value ->> 'importedAt', '')::timestamp with time zone
from xtracker_raw_api_snapshot snapshot
cross join lateral jsonb_array_elements(coalesce(snapshot.response_body -> 'data', snapshot.response_body -> 'posts', '[]'::jsonb)) item(value)
where post.raw_snapshot_id = snapshot.id
  and post.tracker_post_id is null
  and item.value ->> 'id' = post.source_post_id;

update xtracker_crawled_post
set platform_post_id = source_post_id
where platform_post_id is null;

create unique index if not exists uk_xtracker_crawled_post_person_platform_post
    on xtracker_crawled_post (person_id, platform_post_id)
    where platform_post_id is not null;

comment on column xtracker_crawled_post.tracker_post_id is 'XTracker internal post identifier returned as id.';
comment on column xtracker_crawled_post.platform_post_id is 'Source platform post identifier, e.g. X status id returned as platformId.';
comment on column xtracker_crawled_post.imported_at is 'Timestamp when XTracker imported the post.';

create table if not exists xtracker_backfill_job
(
    id                    uuid primary key,
    platform              varchar(30)              not null,
    handle                varchar(100)             not null,
    earliest_at           timestamp with time zone not null,
    cutoff_at             timestamp with time zone not null,
    enable_realtime_after boolean                  not null default true,
    realtime_task_id      uuid references scheduled_task (id),
    status                varchar(30)              not null,
    total_windows         integer                  not null default 0,
    completed_windows     integer                  not null default 0,
    failed_windows        integer                  not null default 0,
    error_message         text,
    created_at            timestamp with time zone not null default now(),
    updated_at            timestamp with time zone not null default now(),
    completed_at          timestamp with time zone
);

create index if not exists idx_xtracker_backfill_job_status
    on xtracker_backfill_job (status, created_at);

create table if not exists xtracker_backfill_window
(
    id               uuid primary key,
    job_id           uuid                     not null references xtracker_backfill_job (id),
    platform         varchar(30)              not null,
    handle           varchar(100)             not null,
    start_at         timestamp with time zone not null,
    end_at           timestamp with time zone not null,
    status           varchar(30)              not null,
    attempt          integer                  not null default 0,
    max_attempts     integer                  not null default 3,
    next_run_at      timestamp with time zone not null default now(),
    lock_owner       varchar(120),
    lock_until       timestamp with time zone,
    last_started_at  timestamp with time zone,
    last_finished_at timestamp with time zone,
    error_message    text,
    created_at       timestamp with time zone not null default now(),
    updated_at       timestamp with time zone not null default now(),
    completed_at     timestamp with time zone,
    constraint ck_xtracker_backfill_window_range check (start_at < end_at)
);

create index if not exists idx_xtracker_backfill_window_due
    on xtracker_backfill_window (status, next_run_at);

create index if not exists idx_xtracker_backfill_window_job
    on xtracker_backfill_window (job_id, status);

comment on table xtracker_backfill_job is 'XTracker historical backfill job tracking records.';
comment on column xtracker_backfill_job.id is 'Backfill job primary key.';
comment on column xtracker_backfill_job.platform is 'Source social platform, e.g. X.';
comment on column xtracker_backfill_job.handle is 'Tracked person handle on the source platform.';
comment on column xtracker_backfill_job.earliest_at is 'Earliest source post timestamp to backfill from.';
comment on column xtracker_backfill_job.cutoff_at is 'Exclusive upper bound captured when the initialization API was called.';
comment on column xtracker_backfill_job.enable_realtime_after is 'Whether to enable realtime polling after all windows complete.';
comment on column xtracker_backfill_job.realtime_task_id is 'Scheduled realtime polling task to enable after completion.';
comment on column xtracker_backfill_job.status is 'Backfill job status.';
comment on column xtracker_backfill_job.total_windows is 'Number of generated backfill windows.';
comment on column xtracker_backfill_job.completed_windows is 'Number of completed backfill windows.';
comment on column xtracker_backfill_job.failed_windows is 'Number of failed backfill windows.';
comment on column xtracker_backfill_job.error_message is 'Last job-level error message.';
comment on column xtracker_backfill_job.created_at is 'Record creation timestamp.';
comment on column xtracker_backfill_job.updated_at is 'Record update timestamp.';
comment on column xtracker_backfill_job.completed_at is 'Timestamp when the job reached a terminal completed state.';

comment on table xtracker_backfill_window is 'XTracker historical backfill time windows.';
comment on column xtracker_backfill_window.id is 'Backfill window primary key.';
comment on column xtracker_backfill_window.job_id is 'Parent xtracker_backfill_job id.';
comment on column xtracker_backfill_window.platform is 'Source social platform, e.g. X.';
comment on column xtracker_backfill_window.handle is 'Tracked person handle on the source platform.';
comment on column xtracker_backfill_window.start_at is 'Inclusive UTC window start.';
comment on column xtracker_backfill_window.end_at is 'Exclusive UTC window end.';
comment on column xtracker_backfill_window.status is 'Backfill window status.';
comment on column xtracker_backfill_window.attempt is 'Number of failed execution attempts.';
comment on column xtracker_backfill_window.max_attempts is 'Maximum execution attempts before marking failed.';
comment on column xtracker_backfill_window.next_run_at is 'Next time this window may be claimed.';
comment on column xtracker_backfill_window.lock_owner is 'Worker instance currently holding this window lock.';
comment on column xtracker_backfill_window.lock_until is 'Lock expiration timestamp for crash recovery.';
comment on column xtracker_backfill_window.last_started_at is 'Most recent execution start timestamp.';
comment on column xtracker_backfill_window.last_finished_at is 'Most recent execution finish timestamp.';
comment on column xtracker_backfill_window.error_message is 'Last window-level error message.';
comment on column xtracker_backfill_window.created_at is 'Record creation timestamp.';
comment on column xtracker_backfill_window.updated_at is 'Record update timestamp.';
comment on column xtracker_backfill_window.completed_at is 'Timestamp when this window completed successfully.';
