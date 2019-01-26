plugins {
    java
    application
    groovy
}

repositories {
    jcenter()
}

dependencies {
    testImplementation("org.codehaus.groovy:groovy-all:2.5.4")
    testImplementation("org.codehaus.groovy.modules.http-builder:http-builder:0.7.1")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5")

    compileOnly("org.projectlombok:lombok:1.18.4")
    annotationProcessor("org.projectlombok:lombok:1.18.4")
}

application {
    mainClassName = "com.example.moneytransfer.App"
}

val fatJar by tasks.creating(Jar::class) {
    manifest {
        attributes["Main-Class"] = application.mainClassName
    }
    archiveBaseName.set("${project.name}-fat")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}

artifacts {
    archives(fatJar)
}