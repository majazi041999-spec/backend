alter table users
    add column if not exists active boolean not null default true;
