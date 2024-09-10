package gymclass.infra.config

import gymclass.app.domain.OutboundPorts
import gymclass.app.usecases.BookClassUseCase
import gymclass.app.usecases.TryToBookForMemberInWaitingListUseCase
import gymclass.app.usecases.CancelBookingUseCase
import gymclass.app.usecases.ExecuteUsecase
import gymclass.app.usecases.SubscribeToWaitingListUseCase

object UseCasesConfig {

    fun bookClassUseCase(
        gymClassRepository: OutboundPorts.GymClassRepository,
        memberFinder: OutboundPorts.MemberFinder,
        eventPublisher: OutboundPorts.EventPublisher,
        executeUsecase: ExecuteUsecase
    ): BookClassUseCase = BookClassUseCase(
        gymClassRepository,
        memberFinder,
        eventPublisher,
        executeUsecase,
    )

    fun subscribeToWaitingListUseCase(
        gymClassRepository: OutboundPorts.GymClassRepository,
        memberFinder: OutboundPorts.MemberFinder,
        eventPublisher: OutboundPorts.EventPublisher,
        executeUsecase: ExecuteUsecase
    ): SubscribeToWaitingListUseCase = SubscribeToWaitingListUseCase(
        gymClassRepository,
        memberFinder,
        eventPublisher,
        executeUsecase,
    )

    fun cancelBookingUseCase(
        gymClassRepository: OutboundPorts.GymClassRepository,
        memberFinder: OutboundPorts.MemberFinder,
        eventPublisher: OutboundPorts.EventPublisher,
        executeUsecase: ExecuteUsecase
    ): CancelBookingUseCase = CancelBookingUseCase(
        gymClassRepository,
        memberFinder,
        eventPublisher,
        executeUsecase,
    )

    fun bookForForWaitingMemberUseCase(
        gymClassRepository: OutboundPorts.GymClassRepository,
        eventPublisher: OutboundPorts.EventPublisher,
        executeUsecase: ExecuteUsecase
    ): TryToBookForMemberInWaitingListUseCase = TryToBookForMemberInWaitingListUseCase(
        gymClassRepository,
        eventPublisher,
        executeUsecase,
    )
}
