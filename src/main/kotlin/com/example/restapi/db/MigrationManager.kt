package com.example.restapi.db

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object MigrationManager {
  private val log = LoggerFactory.getLogger(MigrationManager::class.java)

  fun migrate(
    dataSource: DataSource,
    changeLogPath: String = "db/changelog/db.changelog-master.yaml",
  ) {
    log.info("Menjalankan migrasi database Liquibase dari '{}'...", changeLogPath)
    dataSource.connection.use { connection ->
      val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
      val liquibase = Liquibase(changeLogPath, ClassLoaderResourceAccessor(), database)
      liquibase.update("")
      log.info("Migrasi database Liquibase berhasil selesai")
    }
  }
}
