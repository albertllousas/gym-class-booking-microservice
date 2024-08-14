package gymclass.app.domain

import arrow.core.Either
import gymclass.app.domain.UseCaseErrors.BookClassError
import gymclass.app.domain.UseCaseErrors.BookForWaitingMemberError
import gymclass.app.domain.UseCaseErrors.CancelBookingError
import gymclass.app.domain.UseCaseErrors.SubscribeToWaitingListError
import java.util.UUID

object InboundPorts {

    interface BookClass {
        operator fun invoke(classId: UUID, memberId: UUID): Either<BookClassError, BookingId>
    }

    interface SubscribeToWaitingList {
        operator fun invoke(classId: UUID, memberId: UUID): Either<SubscribeToWaitingListError, Unit>
    }

    interface CancelBooking {
        operator fun invoke(classId: UUID, memberId: UUID): Either<CancelBookingError, Unit>
    }

    interface BookForWaitingMember {
        operator fun invoke(classId: UUID): Either<BookForWaitingMemberError, Unit>
    }
}
