plugins {
    java
    id("org.openapi.generator")
}

openApiGenerate {
    generatorName = "spring"
    inputSpec = layout.projectDirectory.file("src/main/resources/static/openapi.yaml")
    outputDir = layout.buildDirectory.dir("generated/openapi")
    apiPackage = "com.teletronics.storage.api"
    modelPackage = "com.teletronics.storage.api.model"

    configOptions = mapOf(
        "interfaceOnly" to "true",
        "useSpringBoot4" to "true",
        "useJackson3" to "true",
        "useTags" to "true",
        "skipDefaultInterface" to "true",
        "useBeanValidation" to "true",
        "useSpringBuiltInValidation" to "true",
        "openApiNullable" to "false"
    )

    typeMappings = mapOf(
        "file" to "org.springframework.core.io.InputStreamResource"
    )
}

sourceSets {
    main {
        java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
    }
}

tasks.compileJava {
    dependsOn(tasks.named("openApiGenerate"))
}