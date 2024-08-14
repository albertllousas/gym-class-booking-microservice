package gymclass.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import java.util.UUID


fun WireMockServer.stubHttpEnpointForFindMemberNonSucceeded(
    memberId: UUID = DEFAULT_MEMBER_ID,
    responseCode: Int = 400,
    responseErrorBody: String = """{"status":400,"detail":"Some problem"}"""
) =
    this.stubFor(
        get(urlEqualTo("/members/$memberId"))
            .willReturn(status(responseCode).withBody(responseErrorBody))
    )

fun WireMockServer.stubHttpEnpointForFindMemberNotFound(memberId: UUID = DEFAULT_MEMBER_ID) =
    this.stubHttpEnpointForFindMemberNonSucceeded(
        memberId, 404, """ {"status":404,"detail":"Patient not found: $memberId"} """
    )

fun WireMockServer.stubHttpEnpointForFindMemberSucceeded(memberId: UUID = DEFAULT_MEMBER_ID) =
    this.stubFor(
        get(urlEqualTo("/members/$memberId"))
            .willReturn(
                status(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                              "memberId": "$memberId",
                              "fullName": "Jane Doe"
                            }
                        """
                    )
            )
    )
