-- Add summary column to conversations table
ALTER TABLE conversations ADD COLUMN summary TEXT;

-- Add index for better performance on summary queries
CREATE INDEX idx_conversations_summary ON conversations(summary) WHERE summary IS NOT NULL;
