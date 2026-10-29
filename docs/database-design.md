# Database Design

Relational tables:

- `documents`: upload metadata and processing status.
- `chat_sessions`: persisted conversations.
- `chat_messages`: user and assistant messages with timestamps.
- `quizzes` and `quiz_questions`: prepared for saved quiz workflows.

Flyway migration `V1__init.sql` also enables `vector`. Spring AI manages the vector table schema when configured for pgvector; keep embedding dimensions aligned with the selected embedding model.
