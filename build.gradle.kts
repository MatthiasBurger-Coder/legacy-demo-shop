import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.concurrent.ConcurrentHashMap

plugins {
    application
    groovy
    id("de.burger.forensics.btmgen") version "0.0.2-SNAPSHOT"
}

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}


java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

application {
    mainClass.set("com.acme.legacy.LegacyApp")
}

val bytemanAgent = configurations.create("bytemanAgent")

dependencies {

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.13")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")

    // AspectJ
    implementation("org.aspectj:aspectjrt:1.9.24")
    runtimeOnly("org.aspectj:aspectjweaver:1.9.24")      // für 'run'
    testRuntimeOnly("org.aspectj:aspectjweaver:1.9.24")

    // Spock: Groovy-3-Variante
    testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
    testImplementation(localGroovy())

    // optional
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    // JUnit Platform (Spock runs on it)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")

    //Byteman
    add("bytemanAgent", "org.jboss.byteman:byteman:4.0.22")
    testImplementation("org.jboss.byteman:byteman:4.0.22")
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        val weaver = configurations.testRuntimeClasspath.get().files.first { it.name.startsWith("aspectjweaver") }
        jvmArgs("-javaagent:${weaver.absolutePath}")
    }
    dependsOn("generateBtmRules","dedupeBtmRuleNames")
}

tasks.named<JavaExec>("run") {
    // forward project properties to the app's JVM as system properties
    project.findProperty("order.new.enabled")?.let { systemProperty("order.new.enabled", it) }
    project.findProperty("order.new.percent")?.let { systemProperty("order.new.percent", it) }
    //systemProperty("logback.configurationFile", file("src/main/resources/logback.xml").absolutePath)

    doFirst {
        val weaver = configurations.runtimeClasspath.get().files.first { it.name.startsWith("aspectjweaver") }
        jvmArgs("-javaagent:${weaver.absolutePath}")
    }
}

btmGen {
    sourceRoot.set(layout.projectDirectory.dir("src/main/java").asFile)
    outputFile.set(layout.buildDirectory.file("forensics/forensics.btm").get().asFile)
}
tasks.named<de.burger.forensics.plugin.btmgen.gradle.GenerateBtmTask>("generateBtmRules") {
    includeEntryExit.set(false)               // Ersatz für 'entryExit'
    minBranchesPerMethod.set(0)              // Ersatz für 'minBranchesPerMethod'
    logToFile.set(true)                      // Ersatz für 'logToFile'
    logFilePath.set(
        layout.buildDirectory.file("forensics/generate.log").get().asFile.absolutePath
    )
}

tasks.register("dedupeBtmRuleNames") {
    dependsOn("generateBtmRules")
    val inFile = layout.buildDirectory.file("forensics/forensics.btm")
    val outFile = layout.buildDirectory.file("forensics/forensics.dedup.btm")

    inputs.file(inFile)
    outputs.file(outFile)

    doLast {
        val src = inFile.get().asFile
        val dst = outFile.get().asFile
        dst.parentFile.mkdirs()

        // RULE <name>  -> ensure name uniqueness by suffixing duplicates
        val ruleHeader = Regex("""^\s*RULE\s+(.+)\s*$""")
        val seen = ConcurrentHashMap<String, Int>()
        val outLines = src.readLines().map { line ->
            val m = ruleHeader.find(line)
            if (m != null) {
                val orig = m.groupValues[1].trim()
                val n = seen.merge(orig, 1) { a, b -> a + b }!!
                if (n == 1) line else line.replace(orig, "${orig}_$n")
            } else line
        }
        dst.writeText(outLines.joinToString(System.lineSeparator()) + System.lineSeparator())
        println("Deduped BTM written to: ${dst.absolutePath}")
    }
}

tasks.named<Test>("test") {
    dependsOn("dedupeBtmRuleNames")

    doFirst {
        val btm = layout.buildDirectory.file("forensics/forensics.dedup.btm").get().asFile
        val agent = bytemanAgent.resolve().single()

        // Normalize to forward slashes for Windows agent arg parsing
        val btmPath = btm.absolutePath.replace("\\", "/")
        val agentPath = agent.absolutePath.replace("\\", "/")

        // Attach Byteman agent and configure system properties
        jvmArgs(
            // Byteman agent with script and verbose flag
            "-javaagent:$agentPath=script:$btmPath,listener:true,prop:org.jboss.byteman.verbose=true",
            // Make helper package visible to the system class loader
            "-Djboss.modules.system.pkgs=org.jboss.byteman,de.burger.forensics"
        )
    }

    testLogging {
        showStandardStreams = true
        events("PASSED","FAILED","SKIPPED","STANDARD_OUT","STANDARD_ERROR")
        exceptionFormat = TestExceptionFormat.FULL
    }
}



