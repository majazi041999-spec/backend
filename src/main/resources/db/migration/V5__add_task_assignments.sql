alter table tasks
    add column if not exists assigned_to_id bigint,
    add column if not exists created_by_id bigint;

alter table tasks
    add constraint fk_tasks_assigned_to
        foreign key (assigned_to_id) references users(id);

alter table tasks
    add constraint fk_tasks_created_by
        foreign key (created_by_id) references users(id);
