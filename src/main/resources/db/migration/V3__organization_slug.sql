-- Adresse publique et lisible de la page d'une organisation.
ALTER TABLE organization ADD COLUMN slug varchar(200);

UPDATE organization
SET slug = left(trim(both '-' from regexp_replace(lower(f_unaccent(name)), '[^a-z0-9]+', '-', 'g')), 180)
    || '-' || left(id::text, 8);

ALTER TABLE organization ALTER COLUMN slug SET NOT NULL;
ALTER TABLE organization ADD CONSTRAINT organization_slug_key UNIQUE (slug);
