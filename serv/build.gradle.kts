plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Security
//	implementation("org.springframework.boot:spring-boot-starter-security")
//	testImplementation("org.springframework.security:spring-security-test")

	// Lombok
	annotationProcessor("org.projectlombok:lombok")
	compileOnly("org.projectlombok:lombok")

	// MySQL JDBC driver
	runtimeOnly("com.mysql:mysql-connector-j")

	// Online payment
	implementation("com.stripe:stripe-java:27.1.0")

	// Testing
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
