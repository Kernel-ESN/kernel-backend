CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent n'est pas IMMUABLE : l'enveloppe fixe son dictionnaire pour l'utiliser dans un index.
CREATE FUNCTION f_unaccent(text) RETURNS text
  LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
  AS $$ SELECT public.unaccent('public.unaccent', $1) $$;

CREATE TABLE sector (
  id    varchar(40) PRIMARY KEY,
  label varchar(120) NOT NULL
);

CREATE TABLE region (
  id    varchar(40) PRIMARY KEY,
  label varchar(120) NOT NULL
);

INSERT INTO sector (id, label) VALUES
  ('humanitaire', 'Humanitaire et social'),
  ('agriculture', 'Agriculture, élevage, développement rural'),
  ('sante', 'Santé'),
  ('education', 'Éducation, formation, recherche'),
  ('finance', 'Comptabilité, finance, banque, assurance'),
  ('btp', 'BTP et immobilier'),
  ('logistique', 'Logistique et transport'),
  ('eau', 'Eau, hygiène, assainissement'),
  ('commerce', 'Marketing, vente, commerce'),
  ('administration', 'Administration et gestion'),
  ('tourisme', 'Tourisme, hôtellerie, restauration'),
  ('informatique', 'Informatique et numérique'),
  ('services', 'Services généraux'),
  ('mines', 'Mines et énergie');

INSERT INTO region (id, label) VALUES
  ('agadez', 'Agadez'), ('diffa', 'Diffa'), ('dosso', 'Dosso'), ('maradi', 'Maradi'),
  ('niamey', 'Niamey'), ('tahoua', 'Tahoua'), ('tillaberi', 'Tillabéri'), ('zinder', 'Zinder');

CREATE TABLE organization (
  id            uuid PRIMARY KEY,
  name          varchar(160) NOT NULL,
  type          varchar(20)  NOT NULL,
  verification  varchar(20)  NOT NULL,
  contact_phone varchar(30),
  document_path varchar(255),
  document_name varchar(255),
  created_at    timestamptz  NOT NULL
);

CREATE TABLE app_user (
  id              uuid PRIMARY KEY,
  email           varchar(254) NOT NULL UNIQUE,
  password_hash   varchar(100) NOT NULL,
  name            varchar(120) NOT NULL,
  phone           varchar(30),
  role            varchar(20)  NOT NULL,
  organization_id uuid REFERENCES organization (id),
  cv_path         varchar(255),
  cv_name         varchar(255),
  cv_size         bigint,
  cv_updated_at   timestamptz,
  created_at      timestamptz  NOT NULL
);

CREATE TABLE listing (
  id                 uuid PRIMARY KEY,
  slug               varchar(220) NOT NULL UNIQUE,
  universe           varchar(10)  NOT NULL,
  type               varchar(20)  NOT NULL,
  reference          varchar(40),
  title              varchar(140) NOT NULL,
  summary            varchar(300) NOT NULL,
  description        text         NOT NULL,
  organization_id    uuid         NOT NULL REFERENCES organization (id),
  owner_id           uuid REFERENCES app_user (id),
  sector_id          varchar(40) REFERENCES sector (id),
  region_id          varchar(40) REFERENCES region (id),
  city               varchar(80)  NOT NULL,
  country            varchar(2)   NOT NULL DEFAULT 'NE',
  education_level    varchar(10),
  experience_level   varchar(40),
  published_at       timestamptz,
  deadline           timestamptz,
  status             varchar(12)  NOT NULL,
  apply_mode         varchar(10)  NOT NULL,
  apply_email        varchar(254),
  apply_url          varchar(500),
  apply_instructions varchar(1000) NOT NULL,
  budget_amount      numeric(16, 0),
  budget_currency    varchar(8),
  budget_hidden      boolean      NOT NULL DEFAULT false,
  is_sample          boolean      NOT NULL DEFAULT false,
  view_count         bigint       NOT NULL DEFAULT 0,
  updated_at         timestamptz  NOT NULL,
  search             tsvector
);

CREATE INDEX listing_search_idx ON listing USING gin (search);
CREATE INDEX listing_public_idx ON listing (status, deadline);
CREATE INDEX listing_universe_idx ON listing (universe, status, published_at DESC);
CREATE INDEX listing_owner_idx ON listing (owner_id);

-- Recherche en français, sans tenir compte des accents. Le titre pèse plus que le reste.
CREATE FUNCTION listing_search_update() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  organization_name text;
BEGIN
  SELECT name INTO organization_name FROM organization WHERE id = NEW.organization_id;
  NEW.search :=
    setweight(to_tsvector('french', f_unaccent(NEW.title)), 'A') ||
    setweight(to_tsvector('french', f_unaccent(coalesce(organization_name, '') || ' ' || NEW.city)), 'B') ||
    setweight(to_tsvector('french', f_unaccent(NEW.summary || ' ' || NEW.description)), 'C');
  RETURN NEW;
END;
$$;

CREATE TRIGGER listing_search_trigger
  BEFORE INSERT OR UPDATE OF title, summary, description, city, organization_id ON listing
  FOR EACH ROW EXECUTE FUNCTION listing_search_update();

CREATE TABLE listing_attachment (
  listing_id uuid         NOT NULL REFERENCES listing (id) ON DELETE CASCADE,
  position   integer      NOT NULL,
  name       varchar(255) NOT NULL,
  url        varchar(500) NOT NULL,
  size_bytes bigint       NOT NULL,
  PRIMARY KEY (listing_id, position)
);

CREATE TABLE saved_listing (
  id         uuid PRIMARY KEY,
  user_id    uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  listing_id uuid        NOT NULL REFERENCES listing (id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL,
  UNIQUE (user_id, listing_id)
);

CREATE TABLE application (
  id         uuid PRIMARY KEY,
  user_id    uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  listing_id uuid          NOT NULL REFERENCES listing (id) ON DELETE CASCADE,
  status     varchar(12)   NOT NULL,
  letter     varchar(4000) NOT NULL,
  cv_path    varchar(255)  NOT NULL,
  cv_name    varchar(255)  NOT NULL,
  created_at timestamptz   NOT NULL,
  UNIQUE (user_id, listing_id)
);
CREATE INDEX application_listing_idx ON application (listing_id);

CREATE TABLE alert (
  id         uuid PRIMARY KEY,
  user_id    uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  universe   varchar(10),
  type       varchar(20),
  sector_id  varchar(40) REFERENCES sector (id),
  region_id  varchar(40) REFERENCES region (id),
  keywords   varchar(120),
  channel    varchar(10) NOT NULL,
  frequency  varchar(10) NOT NULL,
  active     boolean     NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL
);
CREATE INDEX alert_user_idx ON alert (user_id);
