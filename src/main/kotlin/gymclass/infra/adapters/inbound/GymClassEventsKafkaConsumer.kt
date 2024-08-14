package gymclass.infra.adapters.inbound

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gymclass.app.domain.InboundPorts.BookForWaitingMember
import gymclass.infra.adapters.outbound.ExternalGymClassEvent
import io.helidon.config.Config
import io.helidon.messaging.Channel
import io.helidon.messaging.Messaging
import io.helidon.messaging.connectors.kafka.KafkaConnector

// https://helidon.io/docs/latest/se/reactive-messaging#_kafka_connector
class GymClassEventsKafkaConsumer(
    config: Config,
    private val bookWaitingMember: BookForWaitingMember,
    private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
) {

    init {
        Messaging.builder()
            .config(config)
            .connector(KafkaConnector.create())
            .listener(Channel.create("from-kafka")) { payload: ByteArray -> consume(payload) }
            .build()
            .start()
    }

    private fun consume(payload: ByteArray) {
        when (val event = mapper.readValue(payload, ExternalGymClassEvent::class.java)) {
            is ExternalGymClassEvent.BookingCancelledEvent -> bookWaitingMember(event.gymClass.id) // error
            else -> Unit
        }
    }
}
