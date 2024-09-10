package gymclass.infra.adapters.inbound

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gymclass.app.domain.InboundPorts
import gymclass.fixtures.containers.Kafka
import gymclass.infra.adapters.outbound.BookingDto
import gymclass.infra.adapters.outbound.ExternalGymClassEvent
import gymclass.infra.adapters.outbound.GymClassDto
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import java.time.LocalDateTime
import java.util.UUID

@Tag("integration")
class GymClassEventsKafkaConsumerTest {

    private val kafka = Kafka(Network.newNetwork())

    private val config = Config.builder().also {
        val overriddenProperties = mapOf(
            "mp.messaging.connector.helidon-kafka.bootstrap.servers" to kafka.container.bootstrapServers,
        )
        val customConfigSource = ConfigSources.create(overriddenProperties).build()
        val defaultConfigSource = ConfigSources.classpath("application.yaml").build()
        it.sources(customConfigSource, defaultConfigSource).build()
    }.build()

    private val bookWaitingMember = mockk<InboundPorts.TryToBookForMemberInWaitingList>(relaxed = true)

    private val producer = kafka.buildProducer()

    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `should consume a booking cancelled event and try to book for a waiting member`() {
        val gymClassId = UUID.randomUUID()
        producer.send(
            ProducerRecord(
                kafka.topic,
                objectMapper.writeValueAsString(
                    ExternalGymClassEvent.BookingCancelledEvent(
                        gymClass = GymClassDto(
                            id = gymClassId,
                            name = "Yoga",
                            maxCapacity = 10,
                            startTime = LocalDateTime.now(),
                            endTime = LocalDateTime.now().plusHours(1),
                            bookings = emptyList()
                        ),
                        booking = BookingDto(id = UUID.randomUUID(), memberId = UUID.randomUUID()),
                        occurredOn = LocalDateTime.now(), eventId = UUID.randomUUID()
                    )
                )
            )
        )

        GymClassEventsKafkaConsumer(config, bookWaitingMember)

        verify(timeout = 5000) { bookWaitingMember(gymClassId) }
    }
}