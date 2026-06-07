/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package net.ccbluex.liquidbounce.integration.interop

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerExpectContinueHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpVersion
import net.ccbluex.liquidbounce.utils.client.clientLogger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Local HTTP server that the integration browser talks to.
 *
 * The Svelte theme fetches `http://127.0.0.1:<port>/api/...` to read or
 * write LiquidBounce state. In the minimal WebGUI core the server only
 * serves a small handful of endpoints — the rest of the upstream surface
 * is omitted because we don't expose the marketplace / account manager /
 * OAuth in this build.
 *
 * The server starts lazily the first time we open a browser, on a
 * random free port. The actual port is written into the URL handed to
 * the browser so that it can talk back to us.
 */
object ClientInteropServer {

    private val logger = clientLogger("ClientInteropServer")

    @Volatile var isSkipping: Boolean = false
        private set

    private val started = AtomicBoolean(false)
    private val portRef = AtomicReference(0)
    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var channel: Channel? = null

    val port: Int get() = portRef.get()

    fun start(): Int {
        if (started.get()) return portRef.get()
        if (isSkipping) return 0

        synchronized(this) {
            if (started.get()) return portRef.get()

            return try {
                val boss = NioEventLoopGroup(1)
                val worker = NioEventLoopGroup(2)
                val bootstrap = ServerBootstrap()
                bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(
                                HttpServerCodec(),
                                HttpServerExpectContinueHandler(),
                                HttpObjectAggregator(1 shl 20),
                                InteropHttpHandler(),
                            )
                        }
                    })
                val ch = bootstrap.bind("127.0.0.1", 0).sync().channel()
                val boundPort = (ch.localAddress() as java.net.InetSocketAddress).port
                portRef.set(boundPort)
                bossGroup = boss
                workerGroup = worker
                channel = ch
                started.set(true)
                logger.info("Interop server listening on 127.0.0.1:$boundPort")
                boundPort
            } catch (t: Throwable) {
                logger.error("Failed to start interop server; web UI will be read-only.", t)
                isSkipping = true
                0
            }
        }
    }

    fun stop() {
        if (!started.get()) return
        runCatching { channel?.close()?.sync() }
        runCatching { bossGroup?.shutdownGracefully() }
        runCatching { workerGroup?.shutdownGracefully() }
        started.set(false)
        portRef.set(0)
    }

    /** Cheap healthcheck the browser can hit to know that the client is still alive. */
    fun isReady(): Boolean = started.get()

    private class InteropHttpHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
        override fun channelRead0(ctx: io.netty.channel.ChannelHandlerContext, msg: FullHttpRequest) {
            val path = msg.uri()
            val response = when {
                path.startsWith("/api/ping") -> textResponse("pong")
                path.startsWith("/api/version") -> textResponse("liquidwsa/0.38.1+5")
                else -> notFound()
            }
            ctx.writeAndFlush(response)
        }

        private fun textResponse(body: String): FullHttpResponse {
            val bytes = body.toByteArray(Charsets.UTF_8)
            val resp = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bytes),
            )
            resp.headers().set("Content-Type", "text/plain; charset=utf-8")
            resp.headers().set("Content-Length", bytes.size)
            return resp
        }

        private fun notFound(): FullHttpResponse =
            DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.EMPTY_BUFFER)
    }
}
