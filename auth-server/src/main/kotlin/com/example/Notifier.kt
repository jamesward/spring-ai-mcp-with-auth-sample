package com.example

import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.ott.OneTimeToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service


interface Notifier {
    fun send(token: OneTimeToken, userProfile: UserDetails)
}

@Service
@Profile("!testrun")
class NotifierLive : Notifier {

    override fun send(token: OneTimeToken, userProfile: UserDetails) =
        TODO("Implement your own notifier")

}
