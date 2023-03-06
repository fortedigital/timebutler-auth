CREATE TABLE credentials (
    id serial primary key,
    credential_id varchar not null unique,
    user_id int not null,
    public_key bytea not null,
    transports text[],
    signature_count bigint not null, -- To prevent replays
    attestation_object varchar not null,
    client_data_json json not null,
    created_at timestamptz not null default CURRENT_TIMESTAMP,
    updated_at timestamptz not null default CURRENT_TIMESTAMP,
    constraint fk_users_id foreign key (user_id) references users(id),
    constraint user_credentialid unique (user_id, credential_id)
);