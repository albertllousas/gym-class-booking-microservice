package gymclass.fixtures.containers

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait.forListeningPort

class Postgres(network: Network? = null) {

    val container: KtPostgreSQLContainer = KtPostgreSQLContainer()
        .withNetwork(network?: Network.newNetwork())
        .withCommand("postgres -c wal_level=logical")
        .withNetworkAliases("localhost")
        .withUsername("gym")
        .withPassword("gym")
        .withDatabaseName("gym")
        .also {
            System.setProperty("testcontainers.ryuk.container.timeout", "300")
            it.waitingFor(forListeningPort())
            it.start()
            Flyway(FluentConfiguration().dataSource(it.jdbcUrl, it.username, it.password)).migrate()
        }
}

class KtPostgreSQLContainer : PostgreSQLContainer<KtPostgreSQLContainer>("postgres:latest")
