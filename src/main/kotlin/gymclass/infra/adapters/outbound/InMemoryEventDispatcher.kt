package gymclass.infra.adapters.outbound

import gymclass.app.domain.DomainError
import gymclass.app.domain.DomainEvent
import gymclass.app.domain.OutboundPorts
import kotlin.reflect.KClass

class InMemoryEventDispatcher(
    private val debeziumOutbox: DebeziumOutbox,
    private val metricsEventPublisher: MetricsEventPublisher,
    private val logEventWriter: LogEventWriter,
) : OutboundPorts.EventPublisher, OutboundPorts.ErrorReporter {

    override fun publish(event: DomainEvent) {
        debeziumOutbox.send(event)
        logEventWriter.log(event)
        metricsEventPublisher.publish(event)
    }

    override fun <T : Any> report(error: DomainError, clazz: KClass<T>) {
        logEventWriter.log(error, clazz)
        metricsEventPublisher.publish(error, clazz)
    }

    override fun report(crash: Throwable) {
        logEventWriter.log(crash)
        metricsEventPublisher.publish(crash)
    }
}
