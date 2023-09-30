create table downloading_accounts
(
    id         varchar primary key,
    limitation integer,
    version    integer -- optimistic locking
);

create table downloaded_assets
(
    id                       identity primary key,
    asset_id                 varchar,
    country_code             varchar,
    version                  integer, -- optimistic locking
    account                  varchar references downloading_accounts (id),
    downloading_accounts_key integer, -- to ensure proper ordering of assigned assets
    constraint asset_in_country unique (asset_id, country_code)
);

