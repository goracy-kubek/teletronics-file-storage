plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	id("openapi.generator")
}

group = "com.teletronics"
version = "0.0.1"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation(libs.springdoc.openapi.starter.webmvc.ui)

	// Libs
	implementation(libs.mapstruct)
	annotationProcessor(libs.mapstruct.processor)
	implementation(libs.tika.core)

	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)

	// Test
	testImplementation(libs.assertj.core)
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-mongodb")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
