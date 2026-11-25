plugins {
	java
	id("org.springframework.boot") version "4.1.3"
	id("io.spring.dependency-management") version "1.1.7"
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
		extendsFrom(annotationProcessor.get())
	}
}

repositories {
	gradlePluginPortal()
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	// JWT validation only (the authorization server starter brought unused endpoints and dependencies)
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Outils de dev
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Driver MySQL
	runtimeOnly("com.mysql:mysql-connector-j")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Bibliothèques tierces
	implementation("com.stripe:stripe-java:34.2.0")
	// Stripe's classes carry Gson annotations: needed on the compile classpath to avoid javac warnings
	compileOnly("com.google.code.gson:gson")
	implementation("net.coobird:thumbnailator:0.4.21")
	// WebP decoding for ImageIO (uploaded WebP photos are re-encoded as JPEG)
	implementation("com.twelvemonkeys.imageio:imageio-webp:3.15.3")

	// Tests
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("com.h2database:h2")
}

// Mockito's inline mock maker needs the Byte Buddy agent, declared up front: JDK 21+ warns about (and will
// later forbid) agents attached dynamically at runtime.
val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
	mockitoAgent("net.bytebuddy:byte-buddy-agent") { isTransitive = false }
}

tasks.withType<Test> {
	useJUnitPlatform()
	jvmArgs("-javaagent:${mockitoAgent.asPath}", "-Xshare:off")
}

// Show every compiler warning instead of a one-line summary, so they get fixed.
tasks.withType<JavaCompile> {
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-serial"))
}
