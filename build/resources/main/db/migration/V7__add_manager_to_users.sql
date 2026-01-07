alter table users
    add column if not exists manager_id bigint;

alter table users
    add constraint fk_users_manager
        foreign key (manager_id) references users(id);
