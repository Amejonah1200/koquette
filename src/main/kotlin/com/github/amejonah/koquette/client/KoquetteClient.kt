package com.github.amejonah.koquette.client

import com.github.amejonah.koquette.bytes.buffers.ABCReadingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.ABCSendingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.ReadingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.SendingByteBuffer
import kotlinx.coroutines.flow.flow
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ClientConfig {
  var bufferSize: Int? = null
    set(value) {
      require(value == null || value > 0) { "bufferSize must greater than 0" }
      field = value
    }
}

class KoquetteClient(config: ClientConfig.() -> Unit = {}) {
  private val client: AsynchronousSocketChannel
  val inputByteBuffer: ReadingByteBuffer
  val outputByteBuffer: SendingByteBuffer

  init {
    val config = ClientConfig().apply(config)
    client = AsynchronousSocketChannel.open()
    inputByteBuffer = ABCReadingByteBuffer(config.bufferSize ?: 1024, client)
    outputByteBuffer = ABCSendingByteBuffer(config.bufferSize ?: 1024, client)
  }

  suspend inline fun context(flow: (suspend context(ReadingByteBuffer, SendingByteBuffer) () -> Unit)) {
    flow(inputByteBuffer, outputByteBuffer)
  }

  suspend inline fun readContext(flow: suspend ReadingByteBuffer.() -> Unit) {
    flow(inputByteBuffer)
  }

  suspend inline fun writeContext(flow: suspend SendingByteBuffer.() -> Unit) {
    flow(outputByteBuffer)
  }

  suspend fun connect(address: InetSocketAddress, onConnect: (suspend KoquetteClient.() -> Unit)? = null) {
    suspendCoroutine<Unit> {
      client.connect(address, it, object : CompletionHandler<Void, Continuation<Unit>> {
        override fun completed(result: Void?, attachment: Continuation<Unit>) {
          attachment.resume(Unit)
        }

        override fun failed(exc: Throwable, attachment: Continuation<Unit>) {
          attachment.resumeWithException(exc)
        }
      })
    }
    onConnect?.invoke(this)
  }

  suspend fun connect(host: String, port: Int, onConnect: (suspend KoquetteClient.() -> Unit)? = null) {
    connect(InetSocketAddress(host, port), onConnect)
  }

  suspend fun connect(port: Int, onConnect: (suspend KoquetteClient.() -> Unit)? = null) {
    connect(InetSocketAddress("localhost", port), onConnect)
  }

  fun close() {
    client.close()
  }

}
