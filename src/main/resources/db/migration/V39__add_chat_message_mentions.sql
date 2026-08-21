CREATE TABLE chat_message_mentions (
    message_id NUMBER(19) NOT NULL,
    user_id NUMBER(19) NOT NULL,
    CONSTRAINT fk_chat_message_mention_message
        FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_mention_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT pk_chat_message_mentions PRIMARY KEY (message_id, user_id)
);

CREATE INDEX ix_chat_message_mentions_user ON chat_message_mentions(user_id, message_id);
