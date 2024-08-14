package gymclass.app.usecases

import arrow.core.Either
import gymclass.app.domain.DomainError
import gymclass.app.domain.OutboundPorts
import kotlin.reflect.KClass

class ExecuteUsecase private constructor(
    private val useCaseClass: KClass<*>,
    private val withinTransaction: OutboundPorts.WithinTransaction,
    private val errorReporter: OutboundPorts.ErrorReporter
) {

    companion object {
        fun build(
            useCaseClass: KClass<*>,
            withinTransaction: OutboundPorts.WithinTransaction,
            errorReporter: OutboundPorts.ErrorReporter
        ) = ExecuteUsecase(useCaseClass, withinTransaction, errorReporter)
    }

    operator fun <E : DomainError, R> invoke(execution: () -> Either<E, R>) = withinTransaction {
        try {
            execution().onLeft {
                errorReporter.report(it, useCaseClass)
            }
        } catch (e: Throwable) {
            errorReporter.report(e)
            throw e
        }
    }
}
