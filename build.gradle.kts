import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.7.10"
}

group = "com.github.amejonah1200"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-network-jvm:2.1.1")
  implementation("org.ow2.asm:asm:9.3")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
  kotlinOptions.jvmTarget = "11"
  kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
}
