package gymclass.app.usecases

import arrow.core.Either
import arrow.core.flatMap
import gymclass.app.domain.ClassId
import gymclass.app.domain.InboundPorts
import gymclass.app.domain.OutboundPorts
import gymclass.app.domain.UseCaseErrors.BookForWaitingMemberError
import java.util.UUID

class BookForForWaitingMemberUseCase(
    private val gymClassRepository: OutboundPorts.GymClassRepository,
    private val eventPublisher: OutboundPorts.EventPublisher,
    private val execute: ExecuteUsecase
) : InboundPorts.BookForWaitingMember {

    override fun invoke(classId: UUID): Either<BookForWaitingMemberError, Unit> = execute {
        gymClassRepository.findBy(ClassId(classId))
            .flatMap { it.bookForWaitingMember() }
            .onRight { gymClassRepository.save(it.gymClass) }
            .onRight { eventPublisher.publish(it) }
            .map { Unit }
    }
}
