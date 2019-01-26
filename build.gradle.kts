plugins {
    java
    application
    groovy
}

repositories {
    jcenter()
}

dependencies {
    implementation("com.sparkjava:spark-core:2.8.0")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.fatboyindustrial.gson-javatime-serialisers:gson-javatime-serialisers:1.0.0") {
        exclude(group = "com.google.guava")
    }
    implementation("org.sql2o:sql2o:1.6.0")
    implementation("com.h2database:h2:1.4.197")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.1")

    testImplementation("org.codehaus.groovy:groovy-all:2.5.4")
    testImplementation("org.codehaus.groovy.modules.http-builder:http-builder:0.7.1")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5")
    testImplementation("cglib:cglib-nodep:3.2.10")
    testImplementation("org.objenesis:objenesis:3.0.1")

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