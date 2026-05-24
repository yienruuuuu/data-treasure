alter table xtracker_raw_api_snapshot
    add column if not exists request_url text;

comment on column xtracker_raw_api_snapshot.request_url is 'Full XTracker API URL called by crawler, including query parameters for audit and troubleshooting.';
