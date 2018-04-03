package com.stabbylambda.quietcool

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.router

@Configuration
class RoutingConfiguration {
    @Bean
    fun routes(fan: FanController) = router {
        (accept(APPLICATION_JSON) and "/fans/{ip}").nest {
            GET("/") { fan.listFans(it) }
            "/{uid}".nest {
                POST("/power") { fan.setPower(it) }
                POST("/setCurrentSpeed") { fan.setCurrentSpeed(it)}
                POST("/updateSpeeds") { fan.updateSpeeds(it) }
            }
        }
    }
}

@SpringBootApplication
class QuietcoolApplication

fun main(args: Array<String>) {
    runApplication<QuietcoolApplication>(*args)
}
