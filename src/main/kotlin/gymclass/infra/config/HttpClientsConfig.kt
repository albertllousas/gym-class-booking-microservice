package gymclass.infra.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.helidon.config.Config
import io.helidon.http.media.jackson.JacksonSupport
import io.helidon.webclient.api.WebClient
import java.time.Duration

object HttpClientsConfig {

    fun membersWebClient(config: Config): WebClient =
        WebClient.builder()
            .baseUri(config.get("client.members-api.base-url").asString().get())
            .connectTimeout(Duration.parse(config.get("client.members-api.connect-timeout").asString().get()))
            .readTimeout(Duration.parse(config.get("client.members-api.read-timeout").asString().get()))
            .mediaSupports(listOf(JacksonSupport.create(jacksonObjectMapper())))
            .build()
}