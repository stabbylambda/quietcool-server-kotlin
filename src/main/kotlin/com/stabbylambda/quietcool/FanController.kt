package com.stabbylambda.quietcool

import com.stabbylambda.quietcool.fan.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Mono

fun toFanId(request: ServerRequest) : FanId {
    val ip = request.pathVariable("ip")
    val uid = request.pathVariable("uid")
    return FanId(ip, uid)
}


@Component
class FanController {
    private val log = LoggerFactory.getLogger(FanController::class.java)

    fun setPower(request: ServerRequest): Mono<ServerResponse> {
        val id = toFanId(request);
        return request.bodyToMono<PowerRequest>()
                .flatMap { powerRequest ->
                    log.info("Setting {} power to {}", id.uid, powerRequest.on)
                    power(id, powerRequest)
                }
                .flatMap { response -> ServerResponse.ok().body(BodyInserters.fromObject(response)) }
                .switchIfEmpty(ServerResponse.status(500).syncBody("Error setting power"))
    }

    fun setCurrentSpeed(request: ServerRequest): Mono<ServerResponse> {
        val id = toFanId(request);
        return request.bodyToMono<CurrentSpeedRequest>()
                .flatMap { speedRequest ->
                    log.info("Setting {} current speed to {}", id.uid, speedRequest.speed)
                    setCurrentSpeed(id, speedRequest)
                }
                .flatMap { response -> ServerResponse.ok().body(BodyInserters.fromObject(response)) }
                .switchIfEmpty(ServerResponse.status(500).syncBody("Error setting current speed"))
    }

    fun updateSpeeds(request: ServerRequest): Mono<ServerResponse> {
        val id = toFanId(request);
        return request.bodyToMono<UpdateSpeedsRequest>()
                .flatMap { speedsRequest ->
                    log.info("Setting {} fan speeds to {}", id.uid, speedsRequest.speeds)
                    updateFanSpeeds(id, speedsRequest)
                }
                .flatMap { response -> ServerResponse.ok().body(BodyInserters.fromObject(response))}
                .switchIfEmpty(ServerResponse.status(500).syncBody("Error updating speeds"))
    }

    fun listFans(request: ServerRequest): Mono<ServerResponse> {
        val ip = request.pathVariable("ip")
        return ServerResponse.ok().body(listFansWithInfo(ip))
    }
}