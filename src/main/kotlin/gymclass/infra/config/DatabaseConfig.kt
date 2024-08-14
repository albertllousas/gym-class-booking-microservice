package gymclass.infra.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gymclass.infra.adapters.outbound.DebeziumOutbox
import gymclass.infra.adapters.outbound.GetCurrentTransaction
import gymclass.infra.adapters.outbound.JDBILocalThreadTransactionManager
import io.helidon.config.Config
import org.jdbi.v3.core.Jdbi

object DatabaseConfig {

    fun debeziumOutbox(getCurrentTransaction: GetCurrentTransaction): DebeziumOutbox =
        DebeziumOutbox(currentTransaction = getCurrentTransaction)

    fun jdbiLocalThreadTransactionManager(jdbi: Jdbi): JDBILocalThreadTransactionManager =
        JDBILocalThreadTransactionManager(jdbi)

    fun jdbi(config: Config): Jdbi = Jdbi.create(
        HikariDataSource(
            HikariConfig().apply {
                driverClassName = config.get("database.driver-class-name").asString().get()
                jdbcUrl = config.get("database.jdbc-url").asString().get()
                password = config.get("database.password").asString().get()
                username = config.get("database.username").asString().get()
//                maximumPoolSize = config.get("database.hikari-pool.max-pool-size").asInt().get()
//                isAutoCommit = config.get("database.hikari-pool.auto-commit").asBoolean().get()
//                minimumIdle = config.get("database.hikari-pool.min-idle").asInt().get()
//                idleTimeout = config.get("database.hikari-pool.idle-timeout").asLong().get()
//                maxLifetime = config.get("database.hikari-pool.max-lifetime").asLong().get()
//                connectionTimeout = config.get("database.hikari-pool.connection-timeout").asLong().get()
//                poolName = config.get("database.hikari-pool.pool-name").asString().get()
            }
        )
    )
}
