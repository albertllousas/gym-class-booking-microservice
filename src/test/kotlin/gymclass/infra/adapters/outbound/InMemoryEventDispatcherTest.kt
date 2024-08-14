package gymclass.infra.adapters.outbound

import gymclass.app.domain.DomainFailures
import gymclass.fixtures.TestBuilders.DomainEvents.buildClassBooked
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class InMemoryEventDispatcherTest {

    private val debeziumOutbox = mockk<DebeziumOutbox>(relaxed = true)
    private val metricsEventPublisher = mockk<MetricsEventPublisher>(relaxed = true)
    private val logEventWriter = mockk<LogEventWriter>(relaxed = true)

    private val eventDispatcher = InMemoryEventDispatcher(debeziumOutbox, metricsEventPublisher, logEventWriter)

    @Test
    fun `should publish dispatch domain events`() {
        val event = buildClassBooked()

        eventDispatcher.publish(event)

        verify { debeziumOutbox.send(event) }
        verify { metricsEventPublisher.publish(event) }
        verify { logEventWriter.log(event) }
    }

    @Test
    fun `should report domain errors`() {
        val error = DomainFailures.MaxCapacityReached
        eventDispatcher.report(error, InMemoryEventDispatcherTest::class)

        verify { metricsEventPublisher.publish(error, InMemoryEventDispatcherTest::class) }
        verify { logEventWriter.log(error, InMemoryEventDispatcherTest::class) }
    }

    @Test
    fun `should report application crashes`() {
        val error = Exception("boom")
        eventDispatcher.report(error)

        verify { metricsEventPublisher.publish(error) }
        verify { logEventWriter.log(error) }
    }
}