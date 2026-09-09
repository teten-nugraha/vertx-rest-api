package com.example.restapi

import com.example.restapi.config.AppConfig
import com.example.restapi.controller.HealthController
import com.example.restapi.controller.ItemController
import com.example.restapi.db.DatabaseManager
import com.example.restapi.exception.AppException
import com.example.restapi.repository.ItemRepositoryImpl
import com.example.restapi.response.respondError
import com.example.restapi.service.ItemServiceImpl
import io.vertx.core.Future
import io.vertx.core.VerticleBase
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory

class MainVerticle : VerticleBase() {

  private val log = LoggerFactory.getLogger(MainVerticle::class.java)

  private var databaseManager: DatabaseManager? = null

  override fun start(): Future<*> {
    return AppConfig.load(vertx)
      .compose { config ->
        val dbMgr = DatabaseManager(vertx, config)
        this.databaseManager = dbMgr
        dbMgr.init().map { config }
      }
      .compose { config ->
        val dbMgr = databaseManager!!
        val itemRepository = ItemRepositoryImpl(vertx, dbMgr)
        val itemService = ItemServiceImpl(itemRepository)

        val itemController = ItemController(itemService)
        val healthController = HealthController(config, dbMgr)

        val router = createRouter(healthController, itemController)
        val port = config.serverPort

        vertx.createHttpServer()
          .requestHandler(router)
          .listen(port)
          .onSuccess { server -> log.info("Server REST API berhasil berjalan di port {}", server.actualPort()) }
          .onFailure { err -> log.error("Gagal menjalankan HTTP server: {}", err.message, err) }
      }
  }

  override fun stop(): Future<*> {
    databaseManager?.close()
    return super.stop()
  }

  private fun createRouter(healthController: HealthController, itemController: ItemController): Router {
    val router = Router.router(vertx)

    // Middleware
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // Mount Controllers
    healthController.mount(router)
    itemController.mount(router)

    // 404 Handler untuk URL tidak terdaftar
    router.errorHandler(404) { ctx ->
      ctx.respondError(
        statusCode = 404,
        message = "Endpoint atau resource tidak ditemukan",
        error = "Not Found"
      )
    }

    // Global Failure Handler
    router.route().failureHandler { ctx ->
      val failure = ctx.failure()
      if (failure is AppException) {
        ctx.respondError(failure)
        return@failureHandler
      }

      val statusCode = if (ctx.statusCode() > 0) ctx.statusCode() else 500
      val message = failure?.message ?: "Internal Server Error"
      log.error("Request error: {}", message, failure)

      ctx.respondError(
        statusCode = statusCode,
        message = message,
        error = "Internal Server Error"
      )
    }

    return router
  }
}
