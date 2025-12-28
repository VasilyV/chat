-- V2__fix_auth_tables.sql
-- Align DB schema with JPA entities:
--   User -> chat_user(username PK, password_hash)
--   RefreshToken -> refresh_tokens(token PK, username NOT NULL, created_at NOT NULL)

-- 1) chat_user
CREATE TABLE IF NOT EXISTS public.chat_user (
                                                username VARCHAR(255) PRIMARY KEY
    );

-- Spring's default PhysicalNamingStrategy turns passwordHash -> password_hash
ALTER TABLE public.chat_user
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- The entity also has a unique constraint on username.
-- This is redundant because username is already PRIMARY KEY, but it's safe.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'uk_chat_user_username'
      AND conrelid = 'public.chat_user'::regclass
  ) THEN
ALTER TABLE public.chat_user
    ADD CONSTRAINT uk_chat_user_username UNIQUE (username);
END IF;
END $$;

-- 2) refresh_tokens
CREATE TABLE IF NOT EXISTS public.refresh_tokens (
                                                     token      VARCHAR(255) PRIMARY KEY,
    username   VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
    );

-- If the table existed but columns differ/missing, make it compatible:
ALTER TABLE public.refresh_tokens
    ADD COLUMN IF NOT EXISTS username VARCHAR(255);

ALTER TABLE public.refresh_tokens
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;

-- Ensure NOT NULL as per entity (@Column(nullable=false))
ALTER TABLE public.refresh_tokens
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE public.refresh_tokens
    ALTER COLUMN created_at SET NOT NULL;
