package com.stabbylambda.quietcool.fan

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.beust.klaxon.Klaxon
import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.CoapHandler
import org.eclipse.californium.core.CoapResponse
import org.eclipse.californium.core.coap.CoAP
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.coap.Request
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.nio.charset.Charset

fun <A, B> Option<A>.flatMapNullable(f: (A) -> B?): Option<B> = flatMap { a -> Option.fromNullable(f(a)) }

fun <T : HasUid> Mono<T>.checkFanId(id: FanId): Mono<T> =
        this.filter { it.uid == id.uid}
                .switchIfEmpty(Mono.error(Exception("Stupid controller gave back the wrong fan")))

inline fun <reified T> makeRequest(url: String, request: Request, noinline parseBody: (String) -> T?): Mono<T> =
        Mono.create<T> { source ->
            val client = CoapClient(url)
            val handler = coapHandler(source, parseBody)
            client.advanced(handler, request)
        }

inline fun <reified T> requestObject(url: String, request : Request) =
        makeRequest<T>(url, request) { Klaxon().parse(it)}

inline fun <reified T> requestList(url: String, request : Request) =
        makeRequest<List<T>>(url, request) { Klaxon().parseArray(it) }

inline fun <reified T : HasUid> put(id: FanId, url: String, payload: String) : Mono<T> {
    val request = with(Request(CoAP.Code.PUT)) {
        options.setContentFormat(MediaTypeRegistry.APPLICATION_JSON)
        setPayload(payload)
    }

    return requestObject<T>(url, request).checkFanId(id)
}

inline fun <reified T: HasUid> get(id: FanId, url: String) : Mono<T> =
        requestObject<T>(url, Request(CoAP.Code.GET)).checkFanId(id)

inline fun <reified T> getList(url: String) : Mono<List<T>> =
        requestList(url, Request(CoAP.Code.GET))

inline fun <reified T> coapHandler(source: MonoSink<T>, noinline parse: (payload: String) -> T? ): CoapHandler =
        object : CoapHandler {
            override fun onError() {
                source.error(Exception("Failed to communicate to the fan hub"))
            }

            override fun onLoad(response: CoapResponse?) {
                val result = Option.fromNullable(response)
                        .filter { it.isSuccess }
                        .flatMapNullable { it.payload }
                        .map { it.toString(Charset.defaultCharset()) }
                        .flatMapNullable { parse(it) }

                when (result) {
                    is Some -> source.success(result.t)
                    is None -> source.error(Exception("Somehow failed to parse stuff"))
                }
            }
        }
