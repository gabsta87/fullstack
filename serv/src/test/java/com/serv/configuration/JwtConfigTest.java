package com.serv.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    @Test
    @DisplayName("The application refuses to start without a JWT secret")
    void missingSecret() {
        assertThatThrownBy(() -> JwtConfig.buildKey(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> JwtConfig.buildKey("  ")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("The application refuses a secret shorter than 256 bits")
    void weakSecret() {
        assertThatThrownBy(() -> JwtConfig.buildKey("too-short")).isInstanceOf(IllegalStateException.class);
        assertThat(JwtConfig.buildKey("x".repeat(32)).getAlgorithm()).isEqualTo("HmacSHA256");
    }
}
