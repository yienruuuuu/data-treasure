create table if not exists xtracker_crawled_person
(
    id             uuid primary key,
    platform       varchar(30)              not null,
    handle         varchar(100)             not null,
    source_user_id varchar(128),
    display_name   varchar(255),
    profile_url    text,
    last_seen_at   timestamp with time zone,
    created_at     timestamp with time zone not null default now(),
    updated_at     timestamp with time zone not null default now()
);

create unique index if not exists uk_xtracker_crawled_person_platform_handle
    on xtracker_crawled_person (platform, handle);

create table if not exists xtracker_raw_api_snapshot
(
    id                 uuid primary key,
    source_endpoint    varchar(255)             not null,
    source_object_type varchar(64)              not null,
    source_object_id   varchar(255)             not null,
    request_params     jsonb,
    http_status        integer                  not null,
    response_hash      varchar(64)              not null,
    response_body      jsonb                    not null,
    fetched_at         timestamp with time zone not null default now(),
    created_at         timestamp with time zone not null default now()
);

create index if not exists idx_xtracker_raw_api_snapshot_object_fetched
    on xtracker_raw_api_snapshot (source_object_type, source_object_id, fetched_at desc);

create index if not exists idx_xtracker_raw_api_snapshot_hash
    on xtracker_raw_api_snapshot (source_endpoint, source_object_type, source_object_id, response_hash);

create table if not exists xtracker_crawled_post
(
    id              uuid primary key,
    person_id       uuid                     not null references xtracker_crawled_person (id),
    source_post_id  varchar(128)             not null,
    posted_at       timestamp with time zone not null,
    text            text,
    post_url        text,
    raw_snapshot_id uuid references xtracker_raw_api_snapshot (id),
    content_hash    varchar(64)              not null,
    created_at      timestamp with time zone not null default now(),
    updated_at      timestamp with time zone not null default now()
);

create unique index if not exists uk_xtracker_crawled_post_person_source_post
    on xtracker_crawled_post (person_id, source_post_id);

create index if not exists idx_xtracker_crawled_post_person_posted_at
    on xtracker_crawled_post (person_id, posted_at desc);

comment on table xtracker_crawled_person is 'XTracker crawler tracked person records.';
comment on column xtracker_crawled_person.id is 'Tracked person primary key.';
comment on column xtracker_crawled_person.platform is 'Source social platform, e.g. X.';
comment on column xtracker_crawled_person.handle is 'Tracked person handle on the source platform.';
comment on column xtracker_crawled_person.source_user_id is 'User identifier returned by XTracker or source platform.';
comment on column xtracker_crawled_person.display_name is 'Display name returned by XTracker.';
comment on column xtracker_crawled_person.profile_url is 'Public profile URL for the tracked person.';
comment on column xtracker_crawled_person.last_seen_at is 'Last time crawler successfully observed this person.';
comment on column xtracker_crawled_person.created_at is 'Record creation timestamp.';
comment on column xtracker_crawled_person.updated_at is 'Record update timestamp.';

comment on table xtracker_raw_api_snapshot is 'Raw XTracker API responses captured by crawler.';
comment on column xtracker_raw_api_snapshot.id is 'Raw API snapshot primary key.';
comment on column xtracker_raw_api_snapshot.source_endpoint is 'XTracker API endpoint path called by crawler.';
comment on column xtracker_raw_api_snapshot.source_object_type is 'Object type represented by this response, e.g. PERSON_POSTS.';
comment on column xtracker_raw_api_snapshot.source_object_id is 'Source object identifier, usually platform and handle.';
comment on column xtracker_raw_api_snapshot.request_params is 'Request query parameters used for the API call.';
comment on column xtracker_raw_api_snapshot.http_status is 'HTTP response status code.';
comment on column xtracker_raw_api_snapshot.response_hash is 'SHA-256 hash of the raw response body.';
comment on column xtracker_raw_api_snapshot.response_body is 'Raw JSON response body returned by XTracker.';
comment on column xtracker_raw_api_snapshot.fetched_at is 'Timestamp when crawler fetched this response.';
comment on column xtracker_raw_api_snapshot.created_at is 'Record creation timestamp.';

comment on table xtracker_crawled_post is 'Normalized XTracker posts for time-window count queries.';
comment on column xtracker_crawled_post.id is 'Crawled post primary key.';
comment on column xtracker_crawled_post.person_id is 'Foreign key to xtracker_crawled_person.';
comment on column xtracker_crawled_post.source_post_id is 'Post identifier returned by XTracker or source platform.';
comment on column xtracker_crawled_post.posted_at is 'Post creation timestamp.';
comment on column xtracker_crawled_post.text is 'Post text content.';
comment on column xtracker_crawled_post.post_url is 'Public URL of the source post.';
comment on column xtracker_crawled_post.raw_snapshot_id is 'Raw API snapshot that last produced or updated this row.';
comment on column xtracker_crawled_post.content_hash is 'SHA-256 hash of normalized post fields.';
comment on column xtracker_crawled_post.created_at is 'Record creation timestamp.';
comment on column xtracker_crawled_post.updated_at is 'Record update timestamp.';
