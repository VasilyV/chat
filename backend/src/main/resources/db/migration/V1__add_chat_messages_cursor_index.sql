-- Adds index for cursor (keyset) pagination:
-- WHERE room_id = ? AND (created_at, id) < (cursorCreatedAt, cursorId)
-- ORDER BY created_at DESC, id DESC

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'chat_messages'
  ) THEN
    EXECUTE '
      CREATE INDEX IF NOT EXISTS idx_chat_messages_room_created_id_desc
      ON public.chat_messages (room_id, created_at DESC, id DESC)
    ';
END IF;
END $$;
