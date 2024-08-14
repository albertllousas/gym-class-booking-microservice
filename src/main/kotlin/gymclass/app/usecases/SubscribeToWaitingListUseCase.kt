package gymclass.app.usecases

import arrow.core.Either
import arrow.core.flatMap
import gymclass.app.domain.ClassId
import gymclass.app.domain.InboundPorts
import gymclass.app.domain.MemberId
import gymclass.app.domain.OutboundPorts
import gymclass.app.domain.UseCaseErrors.SubscribeToWaitingListError
import java.util.UUID

class SubscribeToWaitingListUseCase(
    private val gymClassRepository: OutboundPorts.GymClassRepository,
    private val memberFinder: OutboundPorts.MemberFinder,
    private val eventPublisher: OutboundPorts.EventPublisher,
    private val execute: ExecuteUsecase
) : InboundPorts.SubscribeToWaitingList {

    override fun invoke(classId: UUID, memberId: UUID): Either<SubscribeToWaitingListError, Unit> = execute {
        gymClassRepository.findBy(ClassId(classId))
            .flatMap { memberFinder.findBy(MemberId(memberId)).map { member -> Pair(it, member) } }
            .flatMap { (gymClass, member) -> gymClass.subscribeToWaitingList(member) }
            .onRight { gymClassRepository.save(it.gymClass) }
            .onRight { eventPublisher.publish(it) }
            .map { Unit }
    }
}
