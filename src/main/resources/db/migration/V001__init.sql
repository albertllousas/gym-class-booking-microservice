CREATE TABLE gym_classes
(
    id           UUID        NOT NULL,
    max_capacity BIGINT      NOT NULL,
    cancelled    BOOLEAN     NOT NULL,
    name         TEXT        NOT NULL,
    start_time   TIMESTAMPTZ,
    end_time     TIMESTAMPTZ,
    created      TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    version      BIGINT      NOT NULL,
    CONSTRAINT pk_gym_class PRIMARY KEY (id)
);

CREATE TABLE gym_class_bookings
(
    id           UUID        NOT NULL,
    gym_class_id UUID        NOT NULL,
    member_id    UUID        NOT NULL,
    created      TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT pk_gym_class_booking PRIMARY KEY (id),
    CONSTRAINT fk_gym_class FOREIGN KEY(gym_class_id) REFERENCES gym_classes(id)
);

CREATE TABLE outbox
(
    id                UUID PRIMARY KEY,
    aggregateid       UUID                     NOT NULL,
    aggregatetype     TEXT                     NOT NULL,
    event_type        TEXT                     NOT NULL,
    aggregate_version BIGINT                   NOT NULL,
    payload           BYTEA                    NOT NULL,
    occurred_on       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
