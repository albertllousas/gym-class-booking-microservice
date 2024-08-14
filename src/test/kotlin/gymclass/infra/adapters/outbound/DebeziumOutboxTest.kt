package gymclass.infra.adapters.outbound

import com.fasterxml.jackson.module.kotlin.readValue
import gymclass.fixtures.TestBuilders.DomainEvents.buildClassBooked
import gymclass.fixtures.containers.Debezium
import gymclass.fixtures.containers.Kafka
import gymclass.fixtures.containers.Postgres
import io.kotest.matchers.shouldBe
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Tag("integration")
class DebeziumOutboxTest {

    private val network: Network = Network.newNetwork()

    private val db = Postgres(network)

    private val kafka = Kafka(network)

    private val debezium = Debezium(kafka, db)

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.950Z"), ZoneId.of("UTC"))

    private val jdbi = Jdbi.create(db.container.jdbcUrl, db.container.username, db.container.password)

    private val currentHandle = jdbi.open()

    private val outbox = DebeziumOutbox(clock = clock, currentTransaction = { currentHandle })

    private val consumer = kafka.buildConsumer().also { it.subscribe(listOf(kafka.topic)) }

    @AfterEach
    fun `tear down`() {
        kafka.container.stop()
        db.container.stop()
        debezium.container.stop()
    }

    @Test
    fun `should dispatch domain event to the outbox table and captured by the CDC tool`() {
        val event = buildClassBooked()

        outbox.send(event)

        Kafka.drain(consumer, 1).first().also {
            val payload = outbox.objectMapper.readValue<ExternalGymClassEvent.GymClassBookedEvent>(it.value())
            payload.eventType shouldBe "gym_class_booked_event"
            val id = payload.gymClass.id
            id shouldBe event.gymClass.id.value
        }
        currentHandle.createQuery("select count(*) from outbox").mapTo(Long::class.java).one() shouldBe 0
    }
}