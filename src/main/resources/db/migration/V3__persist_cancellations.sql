ALTER TABLE coach_drafts ADD COLUMN cancelled boolean NOT NULL DEFAULT false;
ALTER TABLE coach_changes ADD COLUMN cancelled boolean NOT NULL DEFAULT false;
