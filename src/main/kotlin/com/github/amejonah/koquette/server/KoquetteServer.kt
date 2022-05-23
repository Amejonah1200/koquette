package com.github.amejonah.koquette.server

import com.github.amejonah.koquette.bytes.buffers.ABCReadingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.ABCSendingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.ReadingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.SendingByteBuffer
import io.ktor.util.collections.*
import io.ktor.util.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousByteChannel
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import javax.print.attribute.standard.PrinterURI
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ServerConfig {
  var port: Int = 0
    set(value) {
      require(value > 0) { "port must be greater than 0" }
      field = value
    }
  var backlog: Int = 5000
    set(value) {
      require(value > 0) { "backlog must be greater than 0" }
      field = value
    }
  var group: AsynchronousChannelGroup? = null
}

class ClientConnection(val channel: AsynchronousByteChannel, val inputByteBuffer: ReadingByteBuffer, val outputByteBuffer: SendingByteBuffer) {
  suspend inline fun context(flow: suspend context(ReadingByteBuffer, SendingByteBuffer) () -> Unit) = flow(inputByteBuffer, outputByteBuffer)
  suspend inline fun readContext(flow: suspend ReadingByteBuffer.() -> Unit) = flow(inputByteBuffer)
  suspend inline fun writeContext(flow: suspend SendingByteBuffer.() -> Unit) = flow(outputByteBuffer)
}

class KoquetteServer(config: ServerConfig.() -> Unit = {}) {
  private val server: AsynchronousServerSocketChannel
  val port: Int
    get() = server.localAddress.port
  private val _connections = ConcurrentSet<ClientConnection>()
  val connections: Set<ClientConnection> get() = _connections

  init {
    val serverConfig = ServerConfig().apply(config)
    server = serverConfig.group?.let { AsynchronousServerSocketChannel.open(it) } ?: AsynchronousServerSocketChannel.open()
    server.bind(InetSocketAddress(serverConfig.port), serverConfig.backlog)
  }

  suspend fun startAccepting(onAccept: suspend (ClientConnection) -> Unit) {
    while (server.isOpen) {
      val channel: ClientConnection = suspendCoroutine {
        server.accept(it, object : CompletionHandler<AsynchronousSocketChannel, Continuation<ClientConnection?>> {
          override fun completed(result: AsynchronousSocketChannel, attachment: Continuation<ClientConnection?>) {
            val inputByteBuffer = ABCReadingByteBuffer(1024, result)
            val outputByteBuffer = ABCSendingByteBuffer(1024, result)
            val connection = ClientConnection(result, inputByteBuffer, outputByteBuffer).also(_connections::add)
            attachment.resume(connection)
          }

          override fun failed(exc: Throwable, attachment: Continuation<ClientConnection?>) {
            if (exc !is AsynchronousCloseException) attachment.resumeWithException(exc)
            else attachment.resume(null)
          }
        })
      } ?: return
      onAccept(channel)
    }
  }

  suspend fun stop() {
    withContext(Dispatchers.IO) {
      for (con in _connections) {
        if (con.channel.isOpen) con.channel.close()
      }
      _connections.clear()
      server.close()
    }
  }
}
