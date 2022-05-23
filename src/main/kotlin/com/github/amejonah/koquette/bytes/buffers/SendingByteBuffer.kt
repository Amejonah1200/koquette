package com.github.amejonah.koquette.bytes.buffers

import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

open class SendingByteBuffer(bufferSize: Int = 1024, private val sink: suspend (ByteBuffer) -> Int) {

  var bufferSize: Int = bufferSize
    set(value) {
      require(value > 0) { "Buffer size must be greater than 0" }
      field = value
    }

  private val buffer = ConcurrentLinkedQueue<ByteBuffer>()

  suspend fun send(byteBuffer: ByteBuffer): Int? {
    buffer.add(byteBuffer)
    if (buffer.sumOf { it.limit() } >= bufferSize) return flush()
    return null
  }

  suspend fun flush(): Int {
    val byteBuffers = buffer.toList()
    buffer.clear()
    return sink(byteBuffers.flatten())
  }
}

private fun Collection<ByteBuffer>.flatten(): ByteBuffer = ByteBuffer.allocate(sumOf { it.limit() }).also { buffer ->
  forEach(buffer::put)
  buffer.flip()
}



context (SendingByteBuffer)
  suspend fun Int.send() = send(ByteBuffer.allocate(4).apply { putInt(this@Int); flip() })

context (SendingByteBuffer)
  suspend fun Byte.send() = send(ByteBuffer.allocate(1).apply { put(this@Byte); flip() })

context (SendingByteBuffer)
  suspend fun ByteArray.send1() =
  send(ByteBuffer.allocate(1 + size).apply { put(size.toByte()); put(this@ByteArray); flip() })

context (SendingByteBuffer)
  suspend fun ByteArray.send2() =
  send(ByteBuffer.allocate(2 + size).apply { putShort(size.toShort()); put(this@ByteArray); flip() })

context (SendingByteBuffer)
  suspend fun ByteArray.send4() =
  send(ByteBuffer.allocate(4 + size).apply { putInt(size); put(this@ByteArray); flip() })

context (SendingByteBuffer)
  suspend fun ByteArray.send1u() =
  send(ByteBuffer.allocate(1 + size).apply { put(size.toUByte().toByte()); put(this@ByteArray); flip() })

context (SendingByteBuffer)
  suspend fun ByteArray.send2u() =
  send(ByteBuffer.allocate(2 + size).apply { putShort(size.toUShort().toShort()); put(this@ByteArray); flip() })

context (SendingByteBuffer)
  suspend fun ByteArray.send4u() =
  send(ByteBuffer.allocate(4 + size).apply { putInt(size.toUInt().toInt()); put(this@ByteArray); flip() })

