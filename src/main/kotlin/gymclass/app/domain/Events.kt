package gymclass.app.domain

import java.time.LocalDateTime

sealed interface DomainEvent {
    val gymClass: GymClass
}

data class ClassBooked(val on: LocalDateTime, val booking: Booking, override val gymClass: GymClass, val promotedFromWaitingList: Boolean = false) : DomainEvent

data class MemberSubscribedToWaitingList(val on: LocalDateTime, val member: Member, override val gymClass: GymClass) : DomainEvent

data class BookingCancelled(val on: LocalDateTime, val booking: Booking, override val gymClass: GymClass) : DomainEvent
