package com.example

import org.springaicommunity.mcp.annotation.McpTool
import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer.mcpServerOAuth2
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@SpringBootApplication
class Server {

    @McpTool(description = "add two numbers")
    fun plus(a: Int, b: Int): Int =
        a + b

}

// this bean avoids the startup-resolution of the JWK URI and changes the default security for the /mcp path to just /
@Configuration
@Profile("!test")
class ServerSecurity {
    @Bean
    fun httpSecurityCustomizer(
        resourcesServerProperties: OAuth2ResourceServerProperties,
    ): Customizer<HttpSecurity> =
        Customizer { http ->
            val issuerUri = resourcesServerProperties.jwt.issuerUri!!
            val jwtDecoder = NimbusJwtDecoder.withJwkSetUri("$issuerUri/oauth2/jwks").build()

            http
                .with(mcpServerOAuth2().resourcePath("/")) { auth ->
                    auth.jwtDecoder(jwtDecoder)
                        .authorizationServer(issuerUri)
                }
        }
}

fun main(args: Array<String>) { runApplication<Server>(*args) }
