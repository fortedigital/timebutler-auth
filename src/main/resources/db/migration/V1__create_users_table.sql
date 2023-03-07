create table users (
    id int primary key generated always as identity,
    user_handle uuid not null default gen_random_uuid(),
    username varchar not null unique
);