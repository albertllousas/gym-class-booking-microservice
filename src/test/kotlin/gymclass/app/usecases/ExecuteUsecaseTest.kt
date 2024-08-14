package gymclass.app.usecases

import arrow.core.Either
import arrow.core.left
import gymclass.app.domain.DomainFailures
import gymclass.app.domain.OutboundPorts
import gymclass.fixtures.FakeWithinTransaction
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test


class ExecuteUsecaseTest {

    private val errorReporter = mockk<OutboundPorts.ErrorReporter>(relaxed = true)

    private val execute = ExecuteUsecase.build(
        useCaseClass = SubscribeToWaitingListUseCase::class,
        withinTransaction = FakeWithinTransaction,
        errorReporter = errorReporter
    )

    @Test
    fun `should report a domain error`() {
        val execution: () -> Either<DomainFailures.MaxCapacityReached, Unit> =
            { DomainFailures.MaxCapacityReached.left() }

        execute(execution)

        verify { errorReporter.report(DomainFailures.MaxCapacityReached, SubscribeToWaitingListUseCase::class) }
    }

    @Test
    fun `should report an app crash`() {
        val boom = Exception("boom")
        val execution: () -> Either<DomainFailures.MaxCapacityReached, Unit> = { throw boom }

        shouldThrow<Exception> { execute(execution) }

        verify { errorReporter.report(boom) }
    }
}