package com.example.restapi.db

import com.example.restapi.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory

class DatabaseManager(
  private val vertx: Vertx,
  private val appConfig: AppConfig,
) {
  private val log = LoggerFactory.getLogger(DatabaseManager::class.java)

  private var dataSource: HikariDataSource? = null
  lateinit var dsl: DSLContext
    private set

  fun init(): Future<Void> =
    vertx.executeBlocking<Void> {
      log.info("Menghubungkan ke PostgreSQL di {}...", appConfig.jdbcUrl)
      val config =
        HikariConfig().apply {
          jdbcUrl = appConfig.jdbcUrl
          username = appConfig.dbUser
          password = appConfig.dbPassword
          driverClassName = "org.postgresql.Driver"
          poolName = "VertX-HikariPool"

          maximumPoolSize = appConfig.poolMaxSize
          minimumIdle = appConfig.poolMinIdle
          connectionTimeout = appConfig.poolConnectionTimeoutMs
          idleTimeout = appConfig.poolIdleTimeoutMs
          maxLifetime = appConfig.poolMaxLifetimeMs
          keepaliveTime = appConfig.poolKeepaliveTimeMs
          leakDetectionThreshold = appConfig.poolLeakDetectionThresholdMs
        }

      val ds = HikariDataSource(config)
      this.dataSource = ds
      this.dsl = DSL.using(ds, SQLDialect.POSTGRES)

      MigrationManager.migrate(ds)

      log.info("Koneksi PostgreSQL dan jOOQ DSLContext berhasil diinisialisasi")
      null
    }

  fun checkHealth(): Future<Boolean> =
    vertx.executeBlocking<Boolean> {
      val result = dsl.selectOne().fetchOneInto(Int::class.java)
      result != null && result == 1
    }

  fun close() {
    dataSource?.let {
      if (!it.isClosed) {
        log.info("Menutup HikariCP DataSource...")
        it.close()
      }
    }
  }
}
