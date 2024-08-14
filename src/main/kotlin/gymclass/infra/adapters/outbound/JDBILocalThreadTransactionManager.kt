package gymclass.infra.adapters.outbound

import gymclass.app.domain.OutboundPorts
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class JDBILocalThreadTransactionManager(private val jdbi: Jdbi) : OutboundPorts.WithinTransaction, GetCurrentTransaction {

    private val threadLocalHandle = ThreadLocal<Handle>()

    override fun <T> invoke(transactionalBlock: () -> T): T =
        if (threadLocalHandle.get() != null) {
            transactionalBlock()
        } else {
            try {
                jdbi.inTransaction<T, Exception> { handle ->
                    threadLocalHandle.set(handle)
                    transactionalBlock()
                }
            } finally {
                threadLocalHandle.remove()
            }
        }

    override fun invoke(): Handle = threadLocalHandle.get() ?: throw IllegalStateException("No active JDBI Handle")
}

fun interface GetCurrentTransaction {
  operator fun invoke() : Handle
}
