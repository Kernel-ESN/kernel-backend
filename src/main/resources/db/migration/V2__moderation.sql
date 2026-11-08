ALTER TABLE listing ADD COLUMN moderation_note varchar(500);
ALTER TABLE organization ADD COLUMN verification_note varchar(500);

-- Qui a décidé quoi, et pourquoi. Jamais modifié ni supprimé par l'application.
CREATE TABLE moderation_log (
  id           uuid PRIMARY KEY,
  moderator_id uuid        NOT NULL REFERENCES app_user (id),
  target_type  varchar(20) NOT NULL,
  target_id    uuid        NOT NULL,
  action       varchar(20) NOT NULL,
  note         varchar(500),
  created_at   timestamptz NOT NULL
);
CREATE INDEX moderation_log_target_idx ON moderation_log (target_type, target_id);
