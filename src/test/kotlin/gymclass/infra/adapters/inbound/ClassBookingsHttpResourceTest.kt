package gymclass.infra.adapters.inbound

import arrow.core.left
import arrow.core.right
import gymclass.app.domain.BookingId
import gymclass.app.domain.DomainFailures
import gymclass.app.domain.InboundPorts
import gymclass.app.domain.OutboundPortsErrors.GymClassNotFound
import io.helidon.http.HttpMediaTypes.JSON_UTF_8
import io.helidon.http.Status
import io.helidon.webserver.http.HttpRouting
import io.helidon.webserver.testing.junit5.DirectClient
import io.helidon.webserver.testing.junit5.RoutingTest
import io.helidon.webserver.testing.junit5.SetUpRoute
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import java.util.UUID
import kotlin.test.Test

@Tag("integration")
@RoutingTest
class ClassBookingsHttpResourceTest(private val client: DirectClient) {

    companion object {

        private val bookClass = mockk<InboundPorts.BookClass>()

        private val subscribeToWaitingList = mockk<InboundPorts.SubscribeToWaitingList>()

        private val cancelBooking = mockk<InboundPorts.CancelBooking>()

        @JvmStatic
        @SetUpRoute
        fun routing(builder: HttpRouting.Builder) {
            builder.register(ClassBookingsHttpResource(bookClass, subscribeToWaitingList, cancelBooking))
        }
    }

    @Test
    fun `should handle the booking of a gym class`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val bookingId = BookingId(UUID.randomUUID())
        every { bookClass(classId, memberId) } returns bookingId.right()

        val response = client.post("/classes/$classId/bookings")
            .contentType(JSON_UTF_8)
            .submit("""{"memberId": "$memberId"}""")

        response.status() shouldBe Status.CREATED_201
        response.entity().`as`(Map::class.java) shouldBe mapOf("bookingId" to bookingId.value.toString())
    }

    @Test
    fun `should fail handling the booking of a gym class when booking fails`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { bookClass(classId, memberId) } returns GymClassNotFound.left()

        val response = client.post("/classes/$classId/bookings")
            .contentType(JSON_UTF_8)
            .submit("""{"memberId": "$memberId"}""")

        response.status() shouldBe Status.NOT_FOUND_404
        response.entity().`as`(Map::class.java) shouldBe mapOf("message" to "Gym class not found")
    }

    @Test
    fun `should handle the subscription to the waiting list of a gym class`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { subscribeToWaitingList(classId, memberId) } returns Unit.right()

        val response = client.post("/classes/$classId/waiting-list/$memberId").request()

        response.status() shouldBe Status.CREATED_201
    }

    @Test
    fun `should fail handling the subscription to the waiting list of a gym class when subscription fails`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { subscribeToWaitingList(classId, memberId) } returns DomainFailures.AlreadySubscribedToWaitingList.left()

        val response = client.post("/classes/$classId/waiting-list/$memberId").request()

        response.status() shouldBe Status.CONFLICT_409
        response.entity().`as`(Map::class.java) shouldBe mapOf("message" to "Already subscribed to waiting list")
    }

    @Test
    fun `should handle the cancellation of a booking`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { cancelBooking(classId, memberId) } returns Unit.right()

        val response = client.delete("/classes/$classId/bookings/$memberId").request()

        response.status() shouldBe Status.NO_CONTENT_204
    }

    @Test
    fun `should fail handling the cancellation of a booking when cancellation fails`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { cancelBooking(classId, memberId) } returns DomainFailures.TooLateToCancel.left()

        val response = client.delete("/classes/$classId/bookings/$memberId").request()

        response.status() shouldBe Status.BAD_REQUEST_400
        response.entity().`as`(Map::class.java) shouldBe mapOf("message" to "Too late to cancel")
    }
}
