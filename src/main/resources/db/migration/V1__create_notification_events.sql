CREATE TABLE notification_events (
    event_id        VARCHAR(255)             NOT NULL,
    event_type      VARCHAR(100)             NOT NULL,
    content         TEXT                     NOT NULL,
    delivery_date   TIMESTAMP WITH TIME ZONE NOT NULL,
    delivery_status VARCHAR(50)              NOT NULL,
    client_id       VARCHAR(255)             NOT NULL,
    retry_count     INTEGER                  NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_notification_events PRIMARY KEY (event_id)
);

CREATE INDEX idx_notification_events_client_delivery_status
    ON notification_events (client_id, delivery_date, delivery_status);
