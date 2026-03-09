package com.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class, SecurityFilterAutoConfiguration::class, ServletWebSecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
class TestServer

fun main() {
    runApplication<TestServer>()
}
