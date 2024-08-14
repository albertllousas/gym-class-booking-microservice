package gymclass.component

import com.fasterxml.jackson.core.type.TypeReference
import gymclass.fixtures.containers.Kafka
import gymclass.fixtures.stubHttpEnpointForFindMemberSucceeded
import io.kotest.matchers.shouldBe
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import java.util.UUID

class BookAClassTest : BaseComponentTest() {

    @Test
    fun `should book a class`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        givenAClassExists(classId, 10)
        externalMemberService.stubHttpEnpointForFindMemberSucceeded(memberId)

        val response = given()
            .contentType(ContentType.JSON)
            .body("""{"memberId": "$memberId"}""")
            .port(server.port())
            .`when`()
            .post("/classes/$classId/bookings")
            .then()

        response.extract().statusCode() shouldBe 201
        Kafka.drain(consumer, 1).first().also {
            val typeRef = object : TypeReference<Map<String, Any>>() {}
            val payload = mapper.readValue(it.value(), typeRef)
            payload["event_type"] shouldBe "gym_class_booked_event"
        }
    }
}
