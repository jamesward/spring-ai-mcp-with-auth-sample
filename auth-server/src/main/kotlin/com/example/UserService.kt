package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

interface UserService {
    fun getUser(email: String): User?

    data class User(
        val id: String,

        @get:JsonProperty("user_email")
        val userEmail: String?,
    )
}

@Service
@Profile("!testrun")
class UserServiceLive : UserService {

    override fun getUser(email: String): UserService.User? =
        TODO("Implement your own user service")

}
