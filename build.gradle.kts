plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
    kotlin("plugin.power-assert") version "2.3.20"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    runtimeOnly(kotlin("reflect"))

    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M4"))
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springaicommunity:mcp-server-security-spring-boot:0.1.6")

    testRuntimeOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootTestRun") {
    systemProperty("spring.profiles.active", "test")
}
