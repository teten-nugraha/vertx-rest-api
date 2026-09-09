package com.example.restapi.db

import com.example.restapi.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx

fun main() {
  val vertx = Vertx.vertx()
  try {
    val appConfig =
      AppConfig
        .load(vertx)
        .toCompletionStage()
        .toCompletableFuture()
        .get()
    val hikariConfig =
      HikariConfig().apply {
        jdbcUrl = appConfig.jdbcUrl
        username = appConfig.dbUser
        password = appConfig.dbPassword
        driverClassName = "org.postgresql.Driver"
        poolName = "Liquibase-Cli-Pool"
        maximumPoolSize = 2
      }

    HikariDataSource(hikariConfig).use { ds ->
      MigrationManager.migrate(ds)
      println("Migrasi Liquibase berhasil dijalankan via CLI!")
    }
  } finally {
    vertx.close()
  }
}
