package gymclass.component

import com.fasterxml.jackson.core.type.TypeReference
import gymclass.fixtures.containers.Kafka
import gymclass.fixtures.stubHttpEnpointForFindMemberSucceeded
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import java.util.UUID

class BookAWaitingMemberTest : BaseComponentTest() {

    @Test
    fun `should book for a waiting member when a booking is cancelled for a gym class`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        givenAClassExists(classId, capacity = 1, waitingMember = memberId)
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
        Kafka.drain(consumer, expectedRecordCount = 2).also {
            val typeRef = object : TypeReference<Map<String, Any>>() {}
            val payload = mapper.readValue(it[1].value(), typeRef)
            payload["event_type"] shouldBe "gym_class_booked_event"
        }
    }
}
