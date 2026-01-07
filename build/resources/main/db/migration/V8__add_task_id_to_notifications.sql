alter table in_app_notifications
    add column if not exists task_id bigint references tasks(id) on delete set null;

create index if not exists idx_in_app_notifications_task_id on in_app_notifications(task_id);
