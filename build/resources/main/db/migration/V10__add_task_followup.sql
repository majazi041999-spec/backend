-- Follow-up reminders for task creators/assigners

alter table tasks
    add column if not exists follow_up_enabled boolean not null default false,
    add column if not exists follow_up_at timestamptz;

create table if not exists task_followup_log (
    id bigserial primary key,
    task_id bigint not null references tasks(id) on delete cascade,
    follow_up_at timestamptz not null,
    fired_at timestamptz not null default now(),
    constraint uk_task_followup unique (task_id, follow_up_at)
);
