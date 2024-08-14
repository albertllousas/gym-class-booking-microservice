package gymclass.infra.config

import gymclass.app.domain.OutboundPorts
import gymclass.infra.adapters.outbound.DebeziumOutbox
import gymclass.infra.adapters.outbound.ExternalHttpMemberFinder
import gymclass.infra.adapters.outbound.GetCurrentTransaction
import gymclass.infra.adapters.outbound.InMemoryEventDispatcher
import gymclass.infra.adapters.outbound.LogEventWriter
import gymclass.infra.adapters.outbound.MetricsEventPublisher
import gymclass.infra.adapters.outbound.PostgresqlGymClassRepository
import io.helidon.webclient.api.WebClient
import org.jdbi.v3.core.Jdbi

object OutboundPortsConfig {

    fun membersFinder(webClient: WebClient): OutboundPorts.MemberFinder = ExternalHttpMemberFinder(webClient)

    fun gymClassRepository(
        jdbi: Jdbi,
        getCurrentTransaction: GetCurrentTransaction
    ): OutboundPorts.GymClassRepository = PostgresqlGymClassRepository(jdbi, getCurrentTransaction)

    fun eventPublisher(
        debeziumOutbox: DebeziumOutbox,
        metricsEventPublisher: MetricsEventPublisher,
        logEventWriter: LogEventWriter
    ): InMemoryEventDispatcher = InMemoryEventDispatcher(debeziumOutbox, metricsEventPublisher, logEventWriter)
}