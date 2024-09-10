package gymclass.app.usecases

import arrow.core.Either
import arrow.core.flatMap
import gymclass.app.domain.BookingId
import gymclass.app.domain.ClassId
import gymclass.app.domain.InboundPorts
import gymclass.app.domain.MemberId
import gymclass.app.domain.OutboundPorts
import gymclass.app.domain.UseCaseErrors.BookClassError
import java.util.UUID

class BookClassUseCase(
    private val gymClassRepository: OutboundPorts.GymClassRepository,
    private val memberFinder: OutboundPorts.MemberFinder,
    private val eventPublisher: OutboundPorts.EventPublisher,
    private val execute: ExecuteUsecase
) : InboundPorts.BookClass {

    override fun invoke(classId: UUID, memberId: UUID): Either<BookClassError, BookingId> = execute {
        gymClassRepository.findBy(ClassId(classId))
            .flatMap { memberFinder.findBy(MemberId(memberId)).map { member -> Pair(it, member) } }
            .flatMap { (gymClass, member) -> gymClass.bookFor(member) }
            .onRight { gymClassRepository.save(it.gymClass) }
            .onRight { eventPublisher.publish(it) }
            .map { it.booking.id }
    }
}
