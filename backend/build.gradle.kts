plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.cwowhappy"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.springframework.security:spring-security-crypto:6.3.4")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("com.github.docker-java:docker-java-core:3.7.0")
    testImplementation("com.github.docker-java:docker-java-transport-zerodep:3.7.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

ext["testcontainers.version"] = "1.21.3"

fun loadEnv(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
        val idx = trimmed.indexOf("=")
        if (idx > 0) trimmed.substring(0, idx) to trimmed.substring(idx + 1).trim()
        else null
    }.toMap()
}

tasks.withType<Test> {
    useJUnitPlatform()
    val dotEnv = loadEnv(rootProject.file(".env"))
    dotEnv.forEach { (key, value) ->
        environment(key, value)
    }
    environment("DOCKER_HOST", "unix:///Users/lixiaoyi/.colima/default/docker.sock")
    environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/Users/lixiaoyi/.colima/default/docker.sock")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
