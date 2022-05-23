package com.github.amejonah.koquette

import com.github.amejonah.koquette.bytes.buffers.ReadingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.SendingByteBuffer
import com.github.amejonah.koquette.bytes.buffers.intOrNull
import com.github.amejonah.koquette.bytes.buffers.send4
import com.github.amejonah.koquette.client.KoquetteClient
import com.github.amejonah.koquette.server.ClientConnection
import com.github.amejonah.koquette.server.KoquetteServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.SecureRandom
import kotlin.experimental.and


suspend fun main() {
  coroutineScope {
    val server = KoquetteServer()
    val channel = Channel<ClientConnection>(capacity = Channel.UNLIMITED)
    launch(Dispatchers.IO) {
      for (connection in channel) {
        launch(Dispatchers.IO) {
          println("New connection")
          connection.server()
          println("closing connection")
          server.stop()
        }
      }
    }

    launch(Dispatchers.IO) {
      server.startAccepting {
        println("Accepted!")
        channel.send(it)
      }
      channel.close()
    }
    launch(Dispatchers.IO) {
      val client = KoquetteClient()
      client.connect(server.port)
      client.client()
      client.close()
    }
  }
}


val P = BigInteger.probablePrime(256, SecureRandom())
val G = P / 2.toBigInteger()


private suspend fun KoquetteClient.client() = context {
  println("Client start")
  diffieHellman()?.also { shared ->
    println("C: " + shared.toString(2))
  }
  println("Client stop")
}

private suspend fun ClientConnection.server() = context {
  println("Server start")
  diffieHellman()?.also { shared ->
    println("S: " + shared.toString(2))
  }
  println("Server stop")
}

context(ReadingByteBuffer, SendingByteBuffer)
  private suspend fun diffieHellman(): BigInteger? {
  val a = BigInteger(ByteArray(32).also { SecureRandom().nextBytes(it); it[31] = it[31] and 0b01111111 })
  send(G.modPow(a, P))
  flush()
  val B: BigInteger = readBigInteger() ?: return null
  val s = B.modPow(a, P)
  return s
}

context(SendingByteBuffer)
  suspend fun send(bigInteger: BigInteger) = bigInteger.toByteArray().send4()

context(ReadingByteBuffer)
  suspend fun readBigInteger(): BigInteger? {
  return BigInteger(read(intOrNull() ?: return null).let {
    if (it.remaining() != it.capacity()) null
    else it.array()
  })
}



