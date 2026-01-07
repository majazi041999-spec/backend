-- افزودن سلسله‌مراتب کاربران
alter table users add column if not exists manager_id bigint references users(id);
create index if not exists idx_users_manager_id on users(manager_id);

-- جدول پیام‌های داخل تسک
create table if not exists task_messages (
                                             id bigserial primary key,
                                             task_id bigint not null references tasks(id) on delete cascade,
    sender_id bigint not null references users(id),
    body text not null,
    created_at timestamptz not null default now()
    );

create index if not exists idx_task_messages_task on task_messages(task_id);
