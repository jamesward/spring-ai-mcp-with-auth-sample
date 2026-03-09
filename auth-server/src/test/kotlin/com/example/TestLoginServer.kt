package com.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.ott.OneTimeToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@SpringBootApplication
class TestLoginServer

@Service
@Profile("testrun")
class StdoutNotifier : Notifier {
    override fun send(
        token: OneTimeToken,
        userProfile: UserDetails
    ) {
        println("Login pin: ${token.tokenValue}")
    }
}

@Service
@Profile("testrun")
class UserServiceMock : UserService {
    override fun getUser(email: String): UserService.User? = if (email == "test@example.com") UserService.User("1", email) else null
}

fun main() {
    runApplication<TestLoginServer>()
}
