create table users (
    id serial primary key,
    user_handle uuid not null default gen_random_uuid(),
    username varchar not null
);