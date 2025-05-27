plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com"
version = "0.1"

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
//	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-web:3.3.4")

	// Security
//	implementation("org.springframework.boot:spring-boot-starter-security")
//	testImplementation("org.springframework.security:spring-security-test")
	// implementation("org.springframework.boot:spring-boot-starter-security:3.3.4")
	// testImplementation("org.springframework.security:spring-security-test:6.2.3")

	// Lombok
//	annotationProcessor("org.projectlombok:lombok")
//	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok:1.18.30")
	compileOnly("org.projectlombok:lombok:1.18.30")

	// MySQL JDBC driver
//	runtimeOnly("com.mysql:mysql-connector-j")
	implementation("com.mysql:mysql-connector-j:8.3.0")

	// Online payment
	implementation("com.stripe:stripe-java:27.1.0")

	// Testing
//	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
//	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.4")

	// Sending mails (email confirmation and password reset)
//	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-mail:3.3.4")

	// Authentication
//	implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server:3.3.4")

	// Packing to deploy on one server
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf:3.3.4")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6:3.1.3.RELEASE")
//	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
//	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

	// development tools
//	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-devtools:3.3.4")

	// BCrypt for password hashing
	implementation("org.springframework.security:spring-security-crypto")
	// session management
	implementation("org.springframework.session:spring-session-core")
}

//tasks.withType<Test> {
//	useJUnitPlatform()
//}
