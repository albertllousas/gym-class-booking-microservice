package gymclass.infra.adapters.outbound

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gymclass.app.domain.BookingCancelled
import gymclass.app.domain.ClassBooked
import gymclass.app.domain.DomainEvent
import gymclass.app.domain.GymClass
import gymclass.app.domain.MemberSubscribedToWaitingList
import gymclass.infra.adapters.outbound.ExternalGymClassEvent.BookingCancelledEvent
import gymclass.infra.adapters.outbound.ExternalGymClassEvent.GymClassBookedEvent
import gymclass.infra.adapters.outbound.ExternalGymClassEvent.SubscribedToWaitingListEvent
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class DebeziumOutbox(
    val currentTransaction: GetCurrentTransaction,
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
    private val clock: Clock = Clock.systemUTC(),
    private val generateId: () -> UUID = { UUID.randomUUID() },
) {

    fun send(event: DomainEvent) {
        val externalEvent = event.toGymClassEvent()
        currentTransaction().execute(
            """ INSERT INTO outbox (
                            id,
                            aggregateid,
                            aggregatetype,
                            aggregate_version,
                            event_type,
                            payload,
                            occurred_on
                        ) VALUES (?,?,?,?,?,?,?) """,
            externalEvent.eventId,
            externalEvent.gymClass.id,
            event::class.simpleName,
            event.gymClass.version,
            event::class.simpleName,
            objectMapper.writeValueAsBytes(externalEvent),
            externalEvent.occurredOn
        )
        currentTransaction().execute("DELETE FROM outbox WHERE id = ?", externalEvent.eventId)
    }

    private fun DomainEvent.toGymClassEvent(): ExternalGymClassEvent =
        when (this) {
            is ClassBooked -> GymClassBookedEvent(
                gymClass = this.gymClass.asDto(),
                booking = BookingDto(this.booking.id.value, this.booking.memberId.value),
                occurredOn = LocalDateTime.now(clock),
                eventId = generateId(),
                promotedFromWaitingList = this.promotedFromWaitingList
            )

            is MemberSubscribedToWaitingList -> SubscribedToWaitingListEvent(
                gymClass = this.gymClass.asDto(),
                memberId = this.member.id.value,
                occurredOn = LocalDateTime.now(clock),
                eventId = generateId()
            )

            is BookingCancelled -> BookingCancelledEvent(
                gymClass = this.gymClass.asDto(),
                booking = BookingDto(this.booking.id.value, this.booking.memberId.value),
                occurredOn = LocalDateTime.now(clock),
                eventId = generateId()
            )
        }

    private fun GymClass.asDto() = GymClassDto(
        id = id.value,
        name = name,
        maxCapacity = maxCapacity,
        startTime = startTime,
        endTime = endTime,
        bookings = bookings.map { BookingDto(it.id.value, it.memberId.value) }
    )
}

/*
External event: Event to share changes to other bounded contexts.
*/
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "event_type")
@JsonSubTypes(
    JsonSubTypes.Type(value = GymClassBookedEvent::class, name = "gym_class_booked_event"),
)
sealed class ExternalGymClassEvent(@get:JsonProperty("event_type") val eventType: String) {
    @get:JsonProperty("occurred_on")
    abstract val occurredOn: LocalDateTime

    @get:JsonProperty("event_id")
    abstract val eventId: UUID

    @get:JsonProperty("gym_class")
    abstract val gymClass: GymClassDto

    data class GymClassBookedEvent(
        override val gymClass: GymClassDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
        @get:JsonProperty("promoted_from_waiting_list") val promotedFromWaitingList: Boolean,
        @get:JsonProperty("booking") val booking: BookingDto
    ) : ExternalGymClassEvent("gym_class_booked_event")

    data class BookingCancelledEvent(
        override val gymClass: GymClassDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
        @get:JsonProperty("booking") val booking: BookingDto
    ) : ExternalGymClassEvent("booking_cancelled_event")

    data class SubscribedToWaitingListEvent(
        override val gymClass: GymClassDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
        @get:JsonProperty("member_id") val memberId: UUID
    ) : ExternalGymClassEvent("subscribed_to_waiting_list_event")
}

data class BookingDto(
    @get:JsonProperty("booking_id") val id: UUID,
    @get:JsonProperty("member_id") val memberId: UUID
)

data class GymClassDto(
    @get:JsonProperty("class_id") val id: UUID,
    val name: String,
    @get:JsonProperty("max_capacity") val maxCapacity: Int,
    @get:JsonProperty("start_time") val startTime: LocalDateTime,
    @get:JsonProperty("end_time") val endTime: LocalDateTime,
    val bookings: List<BookingDto>
)
