# Gym Class Booking microservice

Keywords: `Hexagonal-Architecture`, `microservice`, `kotlin`, `Helidon-SE`, `Debezium`, `SOLID`, `Domain-Driven Design`, `functional-programming`,
`Event-Driven Architecture`, `Domain-Events`, `Kafka`, `PostgreSQL`, `Transactional-outbox`, `optimistic-locking`

## Functional requirements

- The system must allow the booking of an existing gym class.
- The system must allow to subscribe to waitlist for a fully booked gym class.
- The system must allow to cancel a booking.
- The system must automatically book the first person in the waitlist when a booking is canceled.

# Pending

- Gym class lyfecycle (CRUD ops): create, update, delete.

## Debezium conf:

It is used only in the test env, for a prod usage it should be configured:

[Debezium test container](/src/test/kotlin/gymclass/fixtures/containers/Debezium.kt)