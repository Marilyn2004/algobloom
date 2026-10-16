-- Additive retry ownership and persistent draft association. Existing conversations are retained.
ALTER TABLE coach_messages ADD COLUMN processing_started_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE coach_messages ADD COLUMN processing_token uuid;
ALTER TABLE coach_messages ADD COLUMN draft_id uuid REFERENCES coach_drafts(id) ON DELETE SET NULL;
CREATE INDEX coach_messages_request_owner ON coach_messages(owner,conversation_id,request_id);
