package gymclass.infra.adapters.outbound

import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import gymclass.app.domain.Member
import gymclass.app.domain.MemberId
import gymclass.app.domain.OutboundPortsErrors.MemberNotFound
import gymclass.fixtures.stubHttpEnpointForFindMemberNonSucceeded
import gymclass.fixtures.stubHttpEnpointForFindMemberNotFound
import gymclass.fixtures.stubHttpEnpointForFindMemberSucceeded
import io.helidon.http.media.jackson.JacksonSupport
import io.helidon.webclient.api.WebClient
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID

@Tag("integration")
class ExternalHttpMemberFinderTest {

    private val externalMemberService = WireMockRule(wireMockConfig().dynamicPort()).also { it.start() }

    private val client = WebClient.builder()
        .baseUri(externalMemberService.baseUrl())
        .mediaSupports(listOf(JacksonSupport.create(jacksonObjectMapper())))
        .build()

    private val patientFinder = ExternalHttpMemberFinder(client)

    @Test
    fun `find a member`() {
        val memberId = UUID.randomUUID()
        externalMemberService.stubHttpEnpointForFindMemberSucceeded(memberId = memberId)

        val result = patientFinder.findBy(MemberId(memberId))

        result shouldBe Member(id = MemberId(memberId)).right()
    }

    @Test
    fun `fail when member does not exists`() {
        val memberId = UUID.randomUUID()
        externalMemberService.stubHttpEnpointForFindMemberNotFound(memberId)

        val result = patientFinder.findBy(MemberId(memberId))

        result shouldBe MemberNotFound.left()
    }

    @Test
    fun `crash when there is a non successful http response`() {
        val memberId = UUID.randomUUID()
        externalMemberService.stubHttpEnpointForFindMemberNonSucceeded(memberId = memberId)

        val exception = shouldThrowExactly<HttpCallNonSucceededException> {
            patientFinder.findBy(MemberId(memberId))
        }
        exception.message shouldBe """Http call with 'ExternalHttpMemberFinder' failed with status '400' and body '{"status":400,"detail":"Some problem"}' """
    }
}
