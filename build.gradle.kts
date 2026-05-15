import java.util.Properties

plugins {
    kotlin("jvm") version "1.9.23"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.nimbusds:oauth2-oidc-sdk:11.19.1")
    implementation("com.nimbusds:nimbus-jose-jwt:9.40")

    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("no.kartverket.matrikkel.token.MainKt")
}

kotlin {
    jvmToolchain(21)
}

val localProperties: Map<String, String> by lazy {
    val f = file("local.properties")
    if (!f.exists()) return@lazy emptyMap()
    val props = Properties()
    props.load(f.reader(Charsets.UTF_8))
    props.entries
        .filter { (_, v) -> v.toString().isNotBlank() }
        .associate { (k, v) -> k.toString() to v.toString() }
}

tasks.withType<JavaExec>().configureEach {
    localProperties.forEach { (k, v) -> systemProperty(k, v) }
}

