package com.stabbylambda.quietcool.fan

import com.beust.klaxon.Klaxon
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.stream.Collectors

interface HasUid {
    val uid: String
}

data class UidOnly(override val uid: String) : HasUid

data class FanId(val ip: String, override val uid: String) : HasUid

data class DeviceResponse(
        override val uid: String,
        val type: String,
        val name: String,
        val version: String,
        val config: String,
        val model: String,
        val pincode: String,
        val role: String,
        val online: String,
        val status: String,
        val hubid: String
) : HasUid

data class ControlResponse(
        override val uid: String,
        val mode: String,
        val sequence: String,
        val speed: String,
        val duration: String,
        val started: String,
        val remaining: String,
        val source: String,
        val input_1_value: String
) : HasUid

data class FanDetails(
        val id: FanId,
        val info: DeviceResponse,
        val status: ControlResponse
)

data class FanList(val fans : List<FanDetails>)

data class TimeRemainingRequest(val remaining: Int)

data class CurrentSpeedRequest(val speed: String)

data class SetSequenceRequest(val sequence: Int)

data class UpdateSpeedsRequest(val speeds: String) {
    fun toSequence(): SetSequenceRequest {
        val sequence = when (this.speeds) {
            "3" -> 0
            "2" -> 1
            "1" -> 4
            else -> {
                0
            }
        }
        return SetSequenceRequest(sequence)
    }
}

data class PowerRequest(val on : Boolean) {
    fun toTimeRemaining(): TimeRemainingRequest {
        val remaining = if(on) 65535 else 0
        return TimeRemainingRequest(remaining)
    }
}

fun deviceUrl(id: FanId) = "coap://${id.ip}/device/${id.uid}"
fun controlUrl(id: FanId) = "coap://${id.ip}/control/${id.uid}"
fun listUrl(ip: String) = "coap://${ip}/uids"

fun getFanInfo(id: FanId) : Mono<DeviceResponse> =
        get(id, deviceUrl(id))

fun getFanStatus(id: FanId) : Mono<ControlResponse> =
        get(id, controlUrl(id))

fun getFanDetails(id: FanId) : Mono<FanDetails> =
        getFanInfo(id).zipWith(getFanStatus(id)) { info, status -> FanDetails(id, info, status) }

fun setTimeRemaining(id: FanId, request: TimeRemainingRequest) : Mono<ControlResponse> =
        put(id, controlUrl(id), Klaxon().toJsonString(request))

fun setCurrentSpeed(id: FanId, request: CurrentSpeedRequest) : Mono<ControlResponse> =
        put(id, controlUrl(id), Klaxon().toJsonString(request))

fun updateFanSpeeds(id: FanId, request: UpdateSpeedsRequest) : Mono<ControlResponse> =
        put(id, controlUrl(id), Klaxon().toJsonString(request.toSequence()))

fun power(id: FanId, request: PowerRequest) : Mono<ControlResponse> =
        setTimeRemaining(id, request.toTimeRemaining())

fun listFansWithInfo(ip: String) : Mono<FanList> {
    return getList<UidOnly>(listUrl(ip))
            .map { it.map { x -> FanId(ip, x.uid) } }
            .flatMapMany { Flux.fromIterable(it) }
            .flatMap { getFanDetails(it) }
            .collect(Collectors.toList())
            .map { it.sortedBy { x -> x.info.hubid } }
            .map { FanList(it) }
}

