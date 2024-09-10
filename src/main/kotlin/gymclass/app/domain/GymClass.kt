package gymclass.app.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import gymclass.app.domain.DomainFailures.*
import gymclass.app.domain.UseCaseErrors.SubscribeToWaitingListError
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

data class GymClass(
    val id: ClassId,
    val name: String,
    val maxCapacity: Int,
    val startTime: LocalDateTime,
    val cancelled: Boolean = false,
    val endTime: LocalDateTime = startTime.plusHours(1),
    val bookings: List<Booking> = emptyList(),
    val waitingList: List<MemberId> = emptyList(),
    val version: Long = 0,
    val clock: Clock = Clock.systemUTC(),
    val generateBookingId: () -> BookingId = GENERATE_BOOKING_ID
) {

    companion object {
        val GENERATE_BOOKING_ID = { BookingId(UUID.randomUUID()) }
    }

    fun bookFor(member: Member): Either<BookClassFailure, ClassBooked> {
        val booking = Booking(generateBookingId(), member.id)
        val result = when {
            startTime.isBefore(now(clock)) || startTime.isEqual(now(clock)) -> TooLateToBook.left()
            bookings.find { it.memberId == member.id } != null -> BookingAlreadyExists.left()
            bookings.size.inc() > maxCapacity -> MaxCapacityReached.left()
//            if there is a waiting list error and member is not the first in the waiting list
            else -> ClassBooked(on = now(clock), booking, gymClass = this.copy(bookings = bookings + booking)).right()
        }
        return result
    }

    fun subscribeToWaitingList(member: Member): Either<SubscribeToWaitingListError, MemberSubscribedToWaitingList> =
        when {
            maxCapacity > bookings.size.inc() -> MaxCapacityNotReached.left()
            bookings.find { it.memberId == member.id } != null -> AlreadySubscribedToClass.left()
            waitingList.contains(member.id) -> AlreadySubscribedToWaitingList.left()
            else -> MemberSubscribedToWaitingList(
                on = now(clock),
                member = member,
                gymClass = this.copy(waitingList = waitingList + member.id)
            ).right()
        }

    fun cancelBookingFor(member: Member): Either<CancelBookingFailure, BookingCancelled> {
        val booking = bookings.find { it.memberId == member.id }
        return when {
            booking == null -> BookingNotFound.left()
            startTime.isBefore(now(clock)) -> TooLateToCancel.left()
            else -> BookingCancelled(
                on = now(clock),
                booking = booking,
                gymClass = this.copy(bookings = bookings - booking)
            ).right()
        }
    }

    fun tryTobookForMemberInWaitingList(): Either<BookForWaitingMemberFailure, ClassBooked> {
        val memberId = waitingList.firstOrNull()
        val result = when {
            memberId == null -> WaitingListEmpty.left()
            maxCapacity > bookings.size.inc() -> MaxCapacityReached.left()
            else -> this.bookFor(member = Member(memberId))
                .mapLeft { it as BookForWaitingMemberFailure }
                .map { it.copy(
                    promotedFromWaitingList = true,
                    gymClass = it.gymClass.copy(waitingList = waitingList - memberId)
                ) }
        }
        return result
    }
}

data class Booking(val id: BookingId, val memberId: MemberId)

data class ClassId(val value: UUID)

data class BookingId(val value: UUID)
