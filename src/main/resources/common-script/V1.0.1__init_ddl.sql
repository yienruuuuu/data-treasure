create table if not exists app_metadata
(
    key        varchar(100) primary key,
    value      text,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
);
