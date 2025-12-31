create table users (
                       id bigserial primary key,
                       full_name varchar(200) not null,
                       email varchar(200) not null unique,
                       password_hash varchar(200) not null,
                       role varchar(30) not null,
                       is_active boolean not null default true,
                       created_at timestamptz not null default now()
);

create table tasks (
                       id bigserial primary key,
                       title varchar(300) not null,
                       description text,
                       priority varchar(10) not null,
                       status varchar(20) not null,
                       due_at timestamptz,
                       assignee_id bigint references users(id),
                       created_by_id bigint not null references users(id),
                       created_at timestamptz not null default now(),
                       updated_at timestamptz not null default now()
);

create table audit_log (
                           id bigserial primary key,
                           actor_user_id bigint references users(id),
                           entity_type varchar(50) not null,
                           entity_id varchar(50) not null,
                           action varchar(50) not null,
                           before_json jsonb,
                           after_json jsonb,
                           ip varchar(100),
                           user_agent varchar(300),
                           created_at timestamptz not null default now()
);
