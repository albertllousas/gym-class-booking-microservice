package gymclass.component

import com.fasterxml.jackson.core.type.TypeReference
import gymclass.fixtures.containers.Kafka
import gymclass.fixtures.stubHttpEnpointForFindMemberSucceeded
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import java.time.Instant.now
import java.util.UUID

class CancelBookingTest : BaseComponentTest() {

    @Test
    fun `should cancel a booking for a gym class`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        givenAClassExists(classId, capacity = 1)
        givenABookingExists(classId, memberId)
        externalMemberService.stubHttpEnpointForFindMemberSucceeded(memberId)

        val response = given()
            .contentType(ContentType.JSON)
            .body("""{"memberId": "$memberId"}""")
            .port(server.port())
            .`when`()
            .delete("/classes/$classId/bookings/$memberId")
            .then()

        response.extract().statusCode() shouldBe 204
        Kafka.drain(consumer, 1).first().also {
            val typeRef = object : TypeReference<Map<String, Any>>() {}
            val payload = mapper.readValue(it.value(), typeRef)
            payload["event_type"] shouldBe "booking_cancelled_event"
        }
    }
}