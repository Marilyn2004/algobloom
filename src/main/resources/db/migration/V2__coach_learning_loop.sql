CREATE TABLE coach_users (
 username varchar(80) PRIMARY KEY, password_hash varchar(255) NOT NULL,
 profile jsonb NOT NULL DEFAULT '{"timezone":"UTC","goal":"","budgetMinutes":30,"analyzeFeelings":false}',
 created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE coach_catalog (
 id integer PRIMARY KEY REFERENCES problems(id), context_version varchar(40) NOT NULL,
 teaching_context text NOT NULL, provenance text NOT NULL, premium boolean NOT NULL DEFAULT false,
 position integer NOT NULL, archived boolean NOT NULL DEFAULT false);
CREATE TABLE coach_conversations (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE coach_drafts (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 conversation_id uuid REFERENCES coach_conversations(id) ON DELETE CASCADE,
 revision bigint NOT NULL DEFAULT 0, budget_minutes integer NOT NULL CHECK(budget_minutes BETWEEN 1 AND 240),
 focus text NOT NULL DEFAULT '', items jsonb NOT NULL, assumptions text NOT NULL DEFAULT '',
 session_id uuid, expires_at timestamptz NOT NULL DEFAULT now()+interval '1 day',
 created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE coach_sessions (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 draft_id uuid UNIQUE NOT NULL REFERENCES coach_drafts(id),
 title text NOT NULL, focus text NOT NULL, timezone varchar(80) NOT NULL,
 state varchar(16) NOT NULL DEFAULT 'active' CHECK(state IN ('active','paused','ended')),
 snapshot jsonb NOT NULL, version bigint NOT NULL DEFAULT 0,
 started_at timestamptz NOT NULL DEFAULT now(), ended_at timestamptz);
CREATE TABLE coach_items (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 session_id uuid NOT NULL REFERENCES coach_sessions(id) ON DELETE CASCADE,
 problem_id integer NOT NULL REFERENCES problems(id), snapshot jsonb NOT NULL,
 position integer NOT NULL, minutes integer NOT NULL CHECK(minutes>0),
 state varchar(20) NOT NULL DEFAULT 'planned',
 UNIQUE(session_id,position));
CREATE TABLE coach_attempts (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 item_id uuid UNIQUE NOT NULL REFERENCES coach_items(id) ON DELETE CASCADE,
 completed boolean NOT NULL DEFAULT false, result varchar(30), actual_minutes integer CHECK(actual_minutes>=0),
 notes text NOT NULL DEFAULT '', feelings text NOT NULL DEFAULT '', version bigint NOT NULL DEFAULT 0,
 highest_hint integer NOT NULL DEFAULT 0 CHECK(highest_hint BETWEEN 0 AND 4),
 first_completed_at timestamptz, updated_at timestamptz NOT NULL DEFAULT now(),
 CHECK(result IS NULL OR result IN ('independent','hinted','solution','understanding')));
CREATE TABLE coach_journals (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 session_id uuid UNIQUE NOT NULL REFERENCES coach_sessions(id) ON DELETE CASCADE,
 title text NOT NULL, body text NOT NULL DEFAULT '', feelings text NOT NULL DEFAULT '',
 version bigint NOT NULL DEFAULT 0, updated_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE coach_memories (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 content text NOT NULL, source varchar(30) NOT NULL DEFAULT 'explicit',
 version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE coach_messages (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 conversation_id uuid NOT NULL REFERENCES coach_conversations(id) ON DELETE CASCADE,
 request_id varchar(100) NOT NULL, content text NOT NULL, intent varchar(20) NOT NULL,
 context jsonb NOT NULL, reply jsonb, status varchar(20) NOT NULL DEFAULT 'pending',
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(conversation_id,request_id));
CREATE TABLE coach_hints (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 attempt_id uuid NOT NULL REFERENCES coach_attempts(id) ON DELETE CASCADE,
 message_id uuid UNIQUE NOT NULL REFERENCES coach_messages(id) ON DELETE CASCADE,
 kind varchar(20) NOT NULL, level integer, context_revision bigint NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX coach_sessions_owner ON coach_sessions(owner,started_at);
CREATE INDEX coach_messages_owner ON coach_messages(owner,conversation_id,created_at);
CREATE INDEX coach_attempts_owner ON coach_attempts(owner,updated_at);

CREATE TABLE coach_changes (
 id uuid PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES coach_users(username) ON DELETE CASCADE,
 session_id uuid NOT NULL REFERENCES coach_sessions(id) ON DELETE CASCADE,
 revision bigint NOT NULL, items jsonb NOT NULL, confirmed boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now());
