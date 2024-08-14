package gymclass.infra.adapters.outbound

import gymclass.app.domain.DomainFailures
import gymclass.fixtures.TestBuilders.DomainEvents.buildClassBooked
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.helpers.NOPLogger

@Tag("integration")
class LogEventWriterTest {

    private val logger = spyk(NOPLogger.NOP_LOGGER)

    private val logEventWriter = LogEventWriter(logger)

    @Test
    fun `log a domain event`() {
        val event = buildClassBooked()

        logEventWriter.log(event)

        verify { logger.info("domain-event: 'ClassBooked', id: '${event.gymClass.id.value}'") }
    }


    @Test
    fun `log a domain error`() {
        val error = DomainFailures.MaxCapacityReached

        logEventWriter.log(error, this::class)

        verify { logger.warn("domain-error: 'MaxCapacityReached', origin: 'LogEventWriterTest'") }
    }

    @Test
    fun `log an application crash`() {
        val boom = Exception("boom")

        logEventWriter.log(boom)

        verify { logger.error("application crash", boom) }
    }
}
