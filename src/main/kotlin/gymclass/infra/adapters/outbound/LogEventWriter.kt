package gymclass.infra.adapters.outbound

import gymclass.app.domain.DomainError
import gymclass.app.domain.DomainEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass

class LogEventWriter(
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass()),
) {

    fun log(event: DomainEvent) {
        val msg = "domain-event: '${event::class.simpleName}', id: '${event.gymClass.id.value}'"
        logger.info(msg)
    }

    fun <T : Any> log(event: DomainError, clazz: KClass<T>) {
        logger.warn("domain-error: '${event::class.simpleName}', origin: '${clazz.simpleName}'")
    }

    fun log(event: Throwable) {
        logger.error("application crash", event)
    }
}
