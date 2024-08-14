package gymclass.infra.adapters.inbound

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import gymclass.app.domain.DomainError
import gymclass.app.domain.DomainFailures.AlreadySubscribedToWaitingList
import gymclass.app.domain.DomainFailures.BookingAlreadyExists
import gymclass.app.domain.DomainFailures.BookingNotFound
import gymclass.app.domain.InboundPorts
import gymclass.app.domain.OutboundPortsErrors.GymClassNotFound
import gymclass.app.domain.OutboundPortsErrors.MemberNotFound
import io.helidon.http.Status.BAD_REQUEST_400
import io.helidon.http.Status.CONFLICT_409
import io.helidon.http.Status.CREATED_201
import io.helidon.http.Status.NOT_FOUND_404
import io.helidon.http.Status.NO_CONTENT_204
import io.helidon.webserver.http.HttpRules
import io.helidon.webserver.http.HttpService
import io.helidon.webserver.http.ServerRequest
import io.helidon.webserver.http.ServerResponse
import java.util.UUID

class ClassBookingsHttpResource(
    private val bookClass: InboundPorts.BookClass,
    private val subscribeToWaitingList: InboundPorts.SubscribeToWaitingList,
    private val cancelBooking: InboundPorts.CancelBooking
) : HttpService {

    override fun routing(rules: HttpRules) {
        rules.post("/classes/{classId}/bookings", this::createBooking)
        rules.delete("/classes/{classId}/bookings/{memberId}", this::cancelBooking)
        rules.post("/classes/{classId}/waiting-list/{memberId}", this::createWaitingListItem)
    }

    fun createBooking(request: ServerRequest, response: ServerResponse) {
        val memberId = request.content().`as`(CreateBookingRequest::class.java).memberId
        val classId = UUID.fromString(request.path().pathParameters().get("classId"))

        bookClass(classId, memberId)
            .onRight { response.status(CREATED_201).send(CreateBookingResponse(it.value) ) }
            .mapLeft { it.asError() }
            .onLeft { response.status(it.first).send(it.second) }
    }

    fun cancelBooking(request: ServerRequest, response: ServerResponse) {
        val memberId = UUID.fromString(request.path().pathParameters().get("memberId"))
        val classId = UUID.fromString(request.path().pathParameters().get("classId"))

        cancelBooking(classId, memberId)
            .onRight { response.status(NO_CONTENT_204).send() }
            .mapLeft { it.asError() }
            .onLeft { response.status(it.first).send(it.second) }
    }

    fun createWaitingListItem(request: ServerRequest, response: ServerResponse) {
        val memberId = UUID.fromString(request.path().pathParameters().get("memberId"))
        val classId = UUID.fromString(request.path().pathParameters().get("classId"))

        subscribeToWaitingList(classId, memberId)
            .onRight { response.status(CREATED_201).send() }
            .mapLeft { it.asError() }
            .onLeft { response.status(it.first).send(it.second) }
    }

    private fun DomainError.asError(): Pair<io.helidon.http.Status, HttpError> = when (this) {
        GymClassNotFound, MemberNotFound, BookingNotFound -> Pair(NOT_FOUND_404, this.toHttpError())
        BookingAlreadyExists, AlreadySubscribedToWaitingList -> Pair(CONFLICT_409, this.toHttpError())
        else -> Pair(BAD_REQUEST_400, this.toHttpError())
    }

    private fun DomainError.toHttpError() = this::class.simpleName!!
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
        .let(::HttpError)
}



data class HttpError(val message: String)

data class CreateBookingRequest @JsonCreator constructor(@JsonProperty("memberId") val memberId: UUID)

data class CreateBookingResponse(val bookingId: UUID)
