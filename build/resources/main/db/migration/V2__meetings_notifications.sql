create table meetings (
                          id bigserial primary key,
                          title varchar(140) not null,
                          date date not null,
                          start_time time,
                          end_time time,
                          all_day boolean not null default true,
                          location varchar(180),
                          content text,
                          outcome text,
                          created_by_id bigint references users(id),
                          created_at timestamptz not null default now(),
                          updated_at timestamptz not null default now()
);

create table meeting_reminders (
                                   meeting_id bigint not null references meetings(id) on delete cascade,
                                   sort_index int not null,
                                   minutes_before int not null,
                                   primary key (meeting_id, sort_index)
);

create index idx_meeting_reminders_meeting_id on meeting_reminders(meeting_id);

create table in_app_notifications (
                                      id bigserial primary key,
                                      type varchar(40) not null,
                                      title varchar(180) not null,
                                      message text,
                                      user_id bigint references users(id),
                                      meeting_id bigint,
                                      created_at timestamptz not null default now(),
                                      read_at timestamptz
);

create index idx_in_app_notifications_user_read on in_app_notifications(user_id, read_at);

create table reminder_log (
                              id bigserial primary key,
                              meeting_id bigint not null references meetings(id) on delete cascade,
                              minutes_before int not null,
                              fired_at timestamptz not null default now(),
                              constraint uk_meeting_minutes unique (meeting_id, minutes_before)
);

create index idx_reminder_log_meeting_id on reminder_log(meeting_id);
