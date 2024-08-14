package gymclass.fixtures

import gymclass.app.domain.OutboundPorts

object FakeWithinTransaction : OutboundPorts.WithinTransaction{
    override fun <T> invoke(transactionalBlock: () -> T): T  = transactionalBlock()
}
