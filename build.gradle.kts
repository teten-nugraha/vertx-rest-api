import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  kotlin("jvm") version "2.2.20"
  application
  id("com.gradleup.shadow") version "9.2.2"
}

group = "com.example.restapi"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "5.1.7"
val junitJupiterVersion = "5.10.2"
val logbackVersion = "1.5.16"
val jooqVersion = "3.19.18"
val postgresqlVersion = "42.7.5"
val hikariVersion = "6.2.0"
val dotenvVersion = "3.1.0"
val liquibaseVersion = "4.31.0"

val mainVerticleName = "com.example.restapi.MainVerticle"
val launcherClassName = "io.vertx.launcher.application.VertxApplication"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-launcher-application")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")

  // Database & jOOQ ORM
  implementation("org.jooq:jooq:$jooqVersion")
  implementation("org.postgresql:postgresql:$postgresqlVersion")
  implementation("com.zaxxer:HikariCP:$hikariVersion")
  implementation("org.liquibase:liquibase-core:$liquibaseVersion")
  implementation("io.github.cdimascio:dotenv-java:$dotenvVersion")
  implementation("org.yaml:snakeyaml:2.0")

  testImplementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("io.vertx:vertx-web-client")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget("21")
    languageVersion = KotlinVersion.fromVersion("2.0")
    apiVersion = KotlinVersion.fromVersion("2.0")
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf(mainVerticleName)
}

tasks.register<JavaExec>("liquibaseUpdate") {
  group = "database"
  description = "Runs Liquibase database migrations"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("com.example.restapi.db.MigrationCliKt")
}
