package com.github.amejonah.koquette.bytes.buffers

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousByteChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

typealias AsynchronousByteChannelReadingByteBuffer = ABCReadingByteBuffer

class ABCReadingByteBuffer(bufferSize: Int, channel: AsynchronousByteChannel) : ReadingByteBuffer(bufferSize, { size ->
  suspendCoroutine { cont ->
    val buffer = ByteBuffer.allocate(size)
    channel.read(buffer, cont, object : CompletionHandler<Int, Continuation<Pair<Int, ByteBuffer>>> {
      override fun completed(result: Int, attachment: Continuation<Pair<Int, ByteBuffer>>) {
        attachment.resume(result to buffer)
      }

      override fun failed(exc: Throwable, attachment: Continuation<Pair<Int, ByteBuffer>>) {
        attachment.resumeWithException(exc)
      }
    })
  }.also { it.second.flip() }
})

typealias AsynchronousByteChannelSendingByteBuffer = ABCSendingByteBuffer

class ABCSendingByteBuffer(bufferSize: Int, channel: AsynchronousByteChannel) : SendingByteBuffer(bufferSize, { buffer ->
  suspendCoroutine { cont ->
    channel.write(buffer, cont, object : CompletionHandler<Int, Continuation<Int>> {
      override fun completed(result: Int, attachment: Continuation<Int>) {
        attachment.resume(result)
      }

      override fun failed(exc: Throwable, attachment: Continuation<Int>) {
        attachment.resumeWithException(exc)
      }
    })
  }
})
