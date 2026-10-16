CREATE TABLE coach_hint_reports (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 message_id uuid NOT NULL REFERENCES coach_messages(id) ON DELETE CASCADE,
 request_id varchar(100) NOT NULL, note text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(owner,message_id,request_id));
