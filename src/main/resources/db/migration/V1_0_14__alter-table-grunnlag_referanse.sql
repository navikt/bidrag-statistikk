
ALTER TABLE grunnlag add column if not exists grunnlagsreferanse_liste text[] DEFAULT array[]::text[] not null;
ALTER TABLE grunnlag add column if not exists gjelder_referanse text;
