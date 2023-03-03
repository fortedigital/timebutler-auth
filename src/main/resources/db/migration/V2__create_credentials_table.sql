CREATE TABLE credentials (
    id serial primary key,
    credential_id varchar not null,
    user_id int not null,
    key_id varchar not null unique,
    public_key bytea not null,
    transports text[],
    signature_count int not null, -- To prevent replays
    attestation_object json not null,
    client_data_json json not null,
    created_at timestamptz not null default CURRENT_TIMESTAMP,
    updated_at timestamptz not null default CURRENT_TIMESTAMP,
    constraint fk_users_id foreign key (user_id) references users(id)
);