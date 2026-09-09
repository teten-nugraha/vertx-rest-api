package com.example.restapi.config

import io.github.cdimascio.dotenv.Dotenv
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Collections

class AppConfig(
  val rawConfig: JsonObject,
) {
  val serverPort: Int
    get() = rawConfig.getJsonObject("server", JsonObject()).getInteger("port", 8080)

  val dbHost: String
    get() = rawConfig.getJsonObject("database", JsonObject()).getString("host", "localhost")

  val dbPort: Int
    get() = rawConfig.getJsonObject("database", JsonObject()).getInteger("port", 5432)

  val dbUser: String
    get() = rawConfig.getJsonObject("database", JsonObject()).getString("user", "root")

  val dbPassword: String
    get() = rawConfig.getJsonObject("database", JsonObject()).getString("password", "")

  val dbName: String
    get() = rawConfig.getJsonObject("database", JsonObject()).getString("dbname", "belajar-vertx-db")

  val jdbcUrl: String
    get() = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

  private val poolConfig: JsonObject
    get() = rawConfig.getJsonObject("database", JsonObject()).getJsonObject("pool", JsonObject())

  val poolMaxSize: Int
    get() = poolConfig.getInteger("maxSize", 10)

  val poolMinIdle: Int
    get() = poolConfig.getInteger("minIdle", 2)

  val poolConnectionTimeoutMs: Long
    get() = (poolConfig.getValue("connectionTimeoutMs") as? Number)?.toLong() ?: 30000L

  val poolIdleTimeoutMs: Long
    get() = (poolConfig.getValue("idleTimeoutMs") as? Number)?.toLong() ?: 600000L

  val poolMaxLifetimeMs: Long
    get() = (poolConfig.getValue("maxLifetimeMs") as? Number)?.toLong() ?: 1800000L

  val poolKeepaliveTimeMs: Long
    get() = (poolConfig.getValue("keepaliveTimeMs") as? Number)?.toLong() ?: 300000L

  val poolLeakDetectionThresholdMs: Long
    get() = (poolConfig.getValue("leakDetectionThresholdMs") as? Number)?.toLong() ?: 0L

  private val jwtConfig: JsonObject
    get() = rawConfig.getJsonObject("jwt", JsonObject())

  val jwtSecret: String
    get() = jwtConfig.getString("secret", "super-secure-jwt-secret-key-for-vertx-api-at-least-256-bits")

  val jwtAccessExpirationMinutes: Long
    get() = (jwtConfig.getValue("accessTokenExpirationMinutes") as? Number)?.toLong() ?: 15L

  val jwtRefreshExpirationDays: Long
    get() = (jwtConfig.getValue("refreshTokenExpirationDays") as? Number)?.toLong() ?: 7L

  companion object {
    private val LOG = LoggerFactory.getLogger(AppConfig::class.java)

    fun load(vertx: Vertx): Future<AppConfig> =
      vertx.executeBlocking<AppConfig> {
        var dotenv: Dotenv? = null
        try {
          dotenv = Dotenv.configure().ignoreIfMissing().load()
        } catch (e: Exception) {
          LOG.debug("File .env tidak ditemukan: {}", e.message)
        }

        var yamlMap: Map<String, Any> = Collections.emptyMap()
        try {
          val yaml = Yaml()
          var inputStream: InputStream? = AppConfig::class.java.classLoader.getResourceAsStream("application.yml")
          if (inputStream == null) {
            val file = File("src/main/resources/application.yml")
            if (file.exists()) {
              inputStream = FileInputStream(file)
            } else {
              val rootFile = File("application.yml")
              if (rootFile.exists()) {
                inputStream = FileInputStream(rootFile)
              }
            }
          }
          if (inputStream != null) {
            inputStream.use { inStream ->
              val loaded: Map<String, Any>? = yaml.load(inStream)
              if (loaded != null) {
                yamlMap = loaded
              }
            }
          }
        } catch (e: Exception) {
          LOG.warn("Gagal memuat application.yml: {}", e.message)
        }

        val config = JsonObject(yamlMap)
        val merged = mergeWithEnv(config, dotenv)
        AppConfig(merged)
      }

    private fun mergeWithEnv(
      config: JsonObject,
      dotenv: Dotenv?,
    ): JsonObject {
      val server = config.getJsonObject("server", JsonObject())
      val database = config.getJsonObject("database", JsonObject())
      val pool = database.getJsonObject("pool", JsonObject())

      val serverPort = getEnvValue("SERVER_PORT", dotenv)
      if (!serverPort.isNullOrBlank()) {
        server.put("port", serverPort.toInt())
      } else if (!server.containsKey("port")) {
        server.put("port", 8080)
      }

      val dbHost = getEnvValue("DB_HOST", dotenv)
      if (!dbHost.isNullOrBlank()) {
        database.put("host", dbHost)
      } else if (!database.containsKey("host")) {
        database.put("host", "localhost")
      }

      val dbPort = getEnvValue("DB_PORT", dotenv)
      if (!dbPort.isNullOrBlank()) {
        database.put("port", dbPort.toInt())
      } else if (!database.containsKey("port")) {
        database.put("port", 5432)
      }

      val dbUser = getEnvValue("DB_USER", dotenv)
      if (!dbUser.isNullOrBlank()) {
        database.put("user", dbUser)
      } else if (!database.containsKey("user")) {
        database.put("user", "root")
      }

      val dbPassword = getEnvValue("DB_PASSWORD", dotenv)
      if (dbPassword != null) {
        database.put("password", dbPassword)
      } else if (!database.containsKey("password")) {
        database.put("password", "")
      }

      val dbName = getEnvValue("DB_NAME", dotenv)
      if (!dbName.isNullOrBlank()) {
        database.put("dbname", dbName)
      } else if (!database.containsKey("dbname")) {
        database.put("dbname", "belajar-vertx-db")
      }

      // HikariCP Pool settings from .env
      getEnvValue("DB_POOL_MAX_SIZE", dotenv)?.toIntOrNull()?.let { pool.put("maxSize", it) }
      getEnvValue("DB_POOL_MIN_IDLE", dotenv)?.toIntOrNull()?.let { pool.put("minIdle", it) }
      getEnvValue("DB_POOL_CONNECTION_TIMEOUT_MS", dotenv)?.toLongOrNull()?.let { pool.put("connectionTimeoutMs", it) }
      getEnvValue("DB_POOL_IDLE_TIMEOUT_MS", dotenv)?.toLongOrNull()?.let { pool.put("idleTimeoutMs", it) }
      getEnvValue("DB_POOL_MAX_LIFETIME_MS", dotenv)?.toLongOrNull()?.let { pool.put("maxLifetimeMs", it) }
      getEnvValue("DB_POOL_KEEPALIVE_TIME_MS", dotenv)?.toLongOrNull()?.let { pool.put("keepaliveTimeMs", it) }
      getEnvValue("DB_POOL_LEAK_DETECTION_THRESHOLD_MS", dotenv)?.toLongOrNull()?.let { pool.put("leakDetectionThresholdMs", it) }

      database.put("pool", pool)

      val jwt = config.getJsonObject("jwt", JsonObject())
      val jwtSecret = getEnvValue("JWT_SECRET", dotenv)
      if (!jwtSecret.isNullOrBlank()) {
        jwt.put("secret", jwtSecret)
      } else if (!jwt.containsKey("secret")) {
        jwt.put("secret", "super-secure-jwt-secret-key-for-vertx-api-at-least-256-bits")
      }

      getEnvValue("JWT_ACCESS_EXPIRATION_MINUTES", dotenv)?.toLongOrNull()?.let {
        jwt.put("accessTokenExpirationMinutes", it)
      }
      getEnvValue("JWT_REFRESH_EXPIRATION_DAYS", dotenv)?.toLongOrNull()?.let {
        jwt.put("refreshTokenExpirationDays", it)
      }

      config.put("server", server)
      config.put("database", database)
      config.put("jwt", jwt)
      return config
    }

    private fun getEnvValue(
      key: String,
      dotenv: Dotenv?,
    ): String? {
      val sysVal = System.getenv(key)
      if (!sysVal.isNullOrBlank()) return sysVal
      return dotenv?.get(key)?.takeIf { it.isNotBlank() }
    }
  }
}
