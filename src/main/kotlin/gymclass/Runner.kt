package gymclass

import gymclass.app.usecases.BookClassUseCase
import gymclass.app.usecases.ExecuteUsecase
import gymclass.app.usecases.SubscribeToWaitingListUseCase
import gymclass.infra.adapters.inbound.ClassBookingsHttpResource
import gymclass.infra.adapters.inbound.GymClassEventsKafkaConsumer
import gymclass.infra.config.DatabaseConfig
import gymclass.infra.config.HttpClientsConfig
import gymclass.infra.config.ObservabilityConfig
import gymclass.infra.config.OutboundPortsConfig
import gymclass.infra.config.UseCasesConfig
import io.helidon.config.Config
import io.helidon.integrations.micrometer.MeterRegistryFactory
import io.helidon.logging.common.LogConfig
import io.helidon.webserver.WebServer
import io.helidon.webserver.http.HttpRouting

fun main() {
    runServer()
}

fun runServer(config: Config = Config.create()): WebServer {
    val server: WebServer = WebServer.builder()
        .config(config["server"])
        .routing(createRoutingAndWireUp(config))
        .build()
    server.start()
    return server
}

fun createRoutingAndWireUp(config: Config): HttpRouting.Builder {
    LogConfig.configureRuntime()
    val meterRegistry = MeterRegistryFactory.getInstance().meterRegistry()
    val membersWebClient = HttpClientsConfig.membersWebClient(config)
    val jdbi = DatabaseConfig.jdbi(config)
    val jdbiLocalThreadTransactionManager = DatabaseConfig.jdbiLocalThreadTransactionManager(jdbi)
    val events = OutboundPortsConfig.eventPublisher(
        debeziumOutbox = DatabaseConfig.debeziumOutbox(jdbiLocalThreadTransactionManager),
        metricsEventPublisher = ObservabilityConfig.metricsEventPublisher(meterRegistry),
        logEventWriter = ObservabilityConfig.logEventWriter()
    )

    val bookClass = UseCasesConfig.bookClassUseCase(
        OutboundPortsConfig.gymClassRepository(jdbi, jdbiLocalThreadTransactionManager),
        OutboundPortsConfig.membersFinder(membersWebClient),
        events,
        ExecuteUsecase.build(BookClassUseCase::class, jdbiLocalThreadTransactionManager, events)
    )

    val subscribeToWaitingList = UseCasesConfig.subscribeToWaitingListUseCase(
        OutboundPortsConfig.gymClassRepository(jdbi, jdbiLocalThreadTransactionManager),
        OutboundPortsConfig.membersFinder(membersWebClient),
        events,
        ExecuteUsecase.build(SubscribeToWaitingListUseCase::class, jdbiLocalThreadTransactionManager, events)
    )

    val cancelBooking = UseCasesConfig.cancelBookingUseCase(
        OutboundPortsConfig.gymClassRepository(jdbi, jdbiLocalThreadTransactionManager),
        OutboundPortsConfig.membersFinder(membersWebClient),
        events,
        ExecuteUsecase.build(SubscribeToWaitingListUseCase::class, jdbiLocalThreadTransactionManager, events)
    )

    val bookForForWaitingMember = UseCasesConfig.bookForForWaitingMemberUseCase(
        OutboundPortsConfig.gymClassRepository(jdbi, jdbiLocalThreadTransactionManager),
        events,
        ExecuteUsecase.build(SubscribeToWaitingListUseCase::class, jdbiLocalThreadTransactionManager, events)
    )

    GymClassEventsKafkaConsumer(config, bookForForWaitingMember)

    val routing: HttpRouting.Builder = HttpRouting
        .builder()
        .register(ClassBookingsHttpResource(bookClass, subscribeToWaitingList, cancelBooking))
    return routing
}
