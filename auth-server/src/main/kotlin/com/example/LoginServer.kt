package com.example

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import gg.jte.generated.precompiled.StaticTemplates
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer.mcpAuthorizationServer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.security.authentication.CachingUserDetailsService
import org.springframework.security.authentication.ott.*
import org.springframework.security.config.Customizer
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.*
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.web.WebAttributes
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.stereotype.Service
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.attributeOrNull
import org.springframework.web.servlet.function.principalOrNull
import org.springframework.web.servlet.function.router
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@SpringBootApplication
class LoginServer {

    private val logger = org.slf4j.LoggerFactory.getLogger(LoginServer::class.java)

    @Bean
    fun routes() = router {
        GET("/") { request ->
            val username = request.principalOrNull()?.name
            if (username == null) {
                val uri = request.uriBuilder().path("/login").build()
                ServerResponse.seeOther(uri).build()
            }
            else {
                val page = Page("Sample Auth Server", "You are logged in")
                val homeContent = StaticTemplates().home(username)
                ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                    .body(StaticTemplates().layout(page, homeContent).render())
            }
        }

        GET("/login") { request ->
            val page = Page("Login", "Please enter your email")
            val csrfToken = request.attributeOrNull(CsrfToken::class.java.name) as? CsrfToken
            val session = request.session()
            val errorMessage = (session.getAttribute("loginError") as? String) ?: run {
                val maybeException = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)
                if (maybeException is InvalidOneTimeTokenException) {
                    session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)
                    "The PIN you entered was invalid."
                }
                else {
                    null
                }
            }
            session.removeAttribute("loginError")
            if (csrfToken == null) {
                throw IllegalStateException("No CSRF token found")
            }
            else {
                val loginContent = StaticTemplates().login(csrfToken.token, errorMessage)
                ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                    .body(StaticTemplates().layout(page, loginContent).render())
            }
        }

        GET("/login/ott") { request ->
            val page = Page("Verify PIN", "Please enter the PIN sent to your email")
            val csrfToken = request.attributeOrNull(CsrfToken::class.java.name) as? CsrfToken
            val session = request.session()
            val message = session.getAttribute("loginMessage") as? String
            session.removeAttribute("loginMessage")
            if (csrfToken == null) {
                throw IllegalStateException("No CSRF token found")
            }
            else {
                val ottContent = StaticTemplates().ott(csrfToken.token, message)
                ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                    .body(StaticTemplates().layout(page, ottContent).render())
            }
        }

        resources("/favicon.ico", ClassPathResource("static/"))

        resources("/static/**", ClassPathResource("static/"))
    }

    val noConsent: ObjectPostProcessor<OAuth2AuthorizationCodeRequestAuthenticationProvider> = object :
        ObjectPostProcessor<OAuth2AuthorizationCodeRequestAuthenticationProvider> {
        override fun <O : OAuth2AuthorizationCodeRequestAuthenticationProvider?> postProcess(
            objectToPostProcess: O?
        ): O? {
            if (objectToPostProcess is OAuth2AuthorizationCodeRequestAuthenticationProvider) {
                objectToPostProcess.setAuthorizationConsentRequired { false }
            }
            return objectToPostProcess
        }
    }

    val longerTTL: ObjectPostProcessor<OAuth2ClientRegistrationAuthenticationProvider> = object :
        ObjectPostProcessor<OAuth2ClientRegistrationAuthenticationProvider> {
        override fun <O : OAuth2ClientRegistrationAuthenticationProvider?> postProcess(
            objectToPostProcess: O?
        ): O? {
            if (objectToPostProcess is OAuth2ClientRegistrationAuthenticationProvider) {
                objectToPostProcess.setRegisteredClientConverter { source ->
                    val registeredClient = OAuth2ClientRegistrationRegisteredClientConverter().convert(source)
                    val tokenSettings = TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofDays(365))
                        .build()
                    RegisteredClient.from(registeredClient).tokenSettings(tokenSettings).build()
                }
            }
            return objectToPostProcess
        }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, cachingUserDetailsService: CachingUserDetailsService, notifier: Notifier): org.springframework.security.web.SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it.requestMatchers("/login", "/login/ott", "/webjars/**", "/static/**", "/favicon.ico").permitAll()
                    .anyRequest().authenticated()
            }
            .with(mcpAuthorizationServer().authorizationServer { authServer ->
                // gets the correct ordering for disabling consent
                authServer.addObjectPostProcessor(noConsent)
                authServer.addObjectPostProcessor(longerTTL)
                // no matter what scopes the client asks for, we are ok with it
                authServer.authorizationEndpoint { endpoint ->
                    endpoint.authenticationProviders { providers ->
                        providers.filterIsInstance<OAuth2AuthorizationCodeRequestAuthenticationProvider>()
                            .forEach { provider ->
                                provider.setAuthenticationValidator { }
                            }
                    }
                }
            }, Customizer.withDefaults())
            .oneTimeTokenLogin {
                it.loginPage("/login")
                    .showDefaultSubmitPage(false)
                    .tokenGenerationSuccessHandler(validUserOnlyOttHandler(cachingUserDetailsService, notifier))
            }

        return http.build()
    }

    class UserDetailsService(val userService: UserService) :
        org.springframework.security.core.userdetails.UserDetailsService {
        override fun loadUserByUsername(username: String): UserDetails =
            userService.getUser(username)?.userEmail?.let { userEmail ->
                User.withUsername(userEmail).roles("USER").build()
            } ?: throw UsernameNotFoundException("User '$username' not found")
    }

    @Bean
    fun userCache(): UserCache = SpringCacheBasedUserCache(ConcurrentMapCache("userCache"))

    @Bean
    fun cachingUserDetailsService(userCache: UserCache, userService: UserService): CachingUserDetailsService {
        val userDetailsService = UserDetailsService(userService)
        val service = CachingUserDetailsService(userDetailsService)
        service.userCache = userCache
        return service
    }

    @Bean
    @ConditionalOnProperty("jwk.rsa.private-key")
    fun jwkSource(@Value($$"${jwk.rsa.private-key}") privateKeyPem: String): JWKSource<SecurityContext> {
        logger.info("Generating JWK from private key")

        val keyBytes = Base64.getDecoder().decode(
            privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
        )
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as RSAPrivateCrtKey
        val publicKey = keyFactory.generatePublic(RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent)) as RSAPublicKey
        val rsaKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyIDFromThumbprint()
            .build()
        return ImmutableJWKSet(JWKSet(rsaKey))
    }

    fun validUserOnlyOttHandler(userDetailsService: CachingUserDetailsService, notifier: Notifier): OneTimeTokenGenerationSuccessHandler {
        return OneTimeTokenGenerationSuccessHandler { request, response, token: OneTimeToken ->
            try {
                val userProfile = userDetailsService.loadUserByUsername(token.username)
                val notified = notifier.send(token, userProfile)
                request.session.setAttribute("loginMessage", "We've emailed a PIN to you.")
                response.sendRedirect("/login/ott")
            } catch (_: UsernameNotFoundException) {
                request.session.setAttribute("loginError", "The email was not found.")
                response.sendRedirect("/login")
            }
        }
    }

}

@Service
class SixDigitOneTimeTokenService : OneTimeTokenService {
    private val tokens = ConcurrentHashMap<String, OneTimeToken>()
    private val random = SecureRandom()

    override fun generate(request: GenerateOneTimeTokenRequest): OneTimeToken {
        val tokenValue = List(6) { random.nextInt(10) }.joinToString("")
        val token = object : OneTimeToken {
            override fun getTokenValue(): String = tokenValue
            override fun getUsername(): String = request.username
            override fun getExpiresAt(): Instant = Instant.now().plusSeconds(600)
        }
        tokens[tokenValue] = token
        return token
    }

    override fun consume(authenticationToken: OneTimeTokenAuthenticationToken): OneTimeToken? {
        return tokens.remove(authenticationToken.tokenValue)
    }
}

data class Page(val title: String, val description: String)

fun main(args: Array<String>) {
    runApplication<LoginServer>(*args)
}
