plugins {
    kotlin("jvm") version "2.2.0"
    application
}

application {
    mainClass.set("com.ivieleague.smbtranslation.MainKt")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val osName = System.getProperty("os.name")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
val targetArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val target = "${targetOs}-${targetArch}"
dependencies {
    implementation("org.jetbrains.skiko:skiko-awt-runtime-$target:0.9.26")
    implementation(kotlin("reflect"))
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1g"
    testLogging {
        showStandardStreams = true
    }
}

tasks.register<JavaExec>("runSmb2j") {
    description = "Run Super Mario Bros. 2 (The Lost Levels) as Mario"
    group = "application"
    mainClass.set("com.ivieleague.smbtranslation.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("smb.variant", "smb2j")
}

tasks.register<JavaExec>("runSmb2jLuigi") {
    description = "Run Super Mario Bros. 2 (The Lost Levels) as Luigi"
    group = "application"
    mainClass.set("com.ivieleague.smbtranslation.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("smb.variant", "smb2j")
    systemProperty("smb.character", "luigi")
}
kotlin {
    jvmToolchain(17)
}