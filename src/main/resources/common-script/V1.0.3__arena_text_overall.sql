create table if not exists arena_text_overall_snapshot
(
    id                   uuid primary key,
    leaderboard_key      varchar(100)             not null,
    source_url           varchar(500)             not null,
    updated_date         date                     not null,
    total_votes          bigint                   not null,
    declared_model_count integer                  not null,
    fetched_model_count  integer                  not null,
    crawl_status         varchar(30)              not null,
    fetched_at           timestamp with time zone not null default now(),
    created_at           timestamp with time zone not null default now()
);

create unique index if not exists uk_arena_text_overall_snapshot_key_date
    on arena_text_overall_snapshot (leaderboard_key, updated_date);

create index if not exists idx_arena_text_overall_snapshot_updated_date
    on arena_text_overall_snapshot (updated_date desc);

create table if not exists arena_text_overall_item
(
    id                 uuid primary key,
    snapshot_id        uuid                     not null references arena_text_overall_snapshot (id),
    rank               integer                  not null,
    rank_spread_min    integer,
    rank_spread_max    integer,
    model_name         varchar(255)             not null,
    provider_name      varchar(255),
    license_type       varchar(255),
    score              integer                  not null,
    score_ci           integer,
    model_votes        integer,
    input_price_per_m  numeric(12, 4),
    output_price_per_m numeric(12, 4),
    context_length_text varchar(64),
    is_preliminary     boolean                  not null default false,
    model_url          text,
    created_at         timestamp with time zone not null default now()
);

create unique index if not exists uk_arena_text_overall_item_snapshot_rank
    on arena_text_overall_item (snapshot_id, rank);

create index if not exists idx_arena_text_overall_item_snapshot_rank
    on arena_text_overall_item (snapshot_id, rank);

comment on table arena_text_overall_snapshot is 'Arena Text Overall leaderboard snapshots.';
comment on column arena_text_overall_snapshot.id is 'Snapshot primary key.';
comment on column arena_text_overall_snapshot.leaderboard_key is 'Leaderboard identifier, fixed as text_overall.';
comment on column arena_text_overall_snapshot.source_url is 'Source leaderboard URL.';
comment on column arena_text_overall_snapshot.updated_date is 'Leaderboard update date shown on page.';
comment on column arena_text_overall_snapshot.total_votes is 'Total votes shown on page.';
comment on column arena_text_overall_snapshot.declared_model_count is 'Expected model count shown on page, e.g. 360 models.';
comment on column arena_text_overall_snapshot.fetched_model_count is 'Actual parsed model rows fetched by crawler.';
comment on column arena_text_overall_snapshot.crawl_status is 'Snapshot crawl status, e.g. SUCCESS or PARTIAL.';
comment on column arena_text_overall_snapshot.fetched_at is 'Timestamp when crawler fetched data.';
comment on column arena_text_overall_snapshot.created_at is 'Snapshot record creation timestamp.';

comment on table arena_text_overall_item is 'Arena Text Overall leaderboard ranking rows for a snapshot.';
comment on column arena_text_overall_item.id is 'Ranking row primary key.';
comment on column arena_text_overall_item.snapshot_id is 'Foreign key to arena_text_overall_snapshot.';
comment on column arena_text_overall_item.rank is 'Leaderboard rank number.';
comment on column arena_text_overall_item.rank_spread_min is 'Minimum rank in rank spread.';
comment on column arena_text_overall_item.rank_spread_max is 'Maximum rank in rank spread.';
comment on column arena_text_overall_item.model_name is 'Model display name.';
comment on column arena_text_overall_item.provider_name is 'Model provider or organization name.';
comment on column arena_text_overall_item.license_type is 'License type label shown on page.';
comment on column arena_text_overall_item.score is 'Model score value.';
comment on column arena_text_overall_item.score_ci is 'Score confidence interval after plus-minus symbol.';
comment on column arena_text_overall_item.model_votes is 'Per-model votes shown in leaderboard row.';
comment on column arena_text_overall_item.input_price_per_m is 'Input token price per 1M tokens.';
comment on column arena_text_overall_item.output_price_per_m is 'Output token price per 1M tokens.';
comment on column arena_text_overall_item.context_length_text is 'Context length raw text, e.g. 1M, 128K, N/A.';
comment on column arena_text_overall_item.is_preliminary is 'Whether row is marked Preliminary.';
comment on column arena_text_overall_item.model_url is 'Model URL linked from row.';
comment on column arena_text_overall_item.created_at is 'Ranking row creation timestamp.';
