package com.github.amejonah.koquette.bytes.buffers

import java.nio.ByteBuffer
import kotlin.math.max

open class ReadingByteBuffer(bufferSize: Int = 1024, private val source: suspend (Int) -> Pair<Int, ByteBuffer>) {
  private var buffer: Buffer? = null
  var bufferSize: Int = bufferSize
    set(value) {
      require(value > 0) { "bufferSize must be positive" }
      field = value
    }

  init {
    require(bufferSize > 0) { "bufferSize must be greater than 0" }
  }

  suspend fun read(size: Int): ByteBuffer {
    require(size > 0) { "size must be positive" }
    if (buffer == null) {
      if (size == bufferSize) return source(size).second
      buffer = Buffer.WholeBuffer(source(max(size, bufferSize)).second)
    }
    if (buffer == Buffer.Empty) return buffer!!.get(size)
    if (buffer!!.remaining() < size) {
      var newBuffer = source(max(size - buffer!!.remaining(), bufferSize)).second
      while (newBuffer.remaining() != 0 && buffer!!.remaining() < size) {
        buffer = buffer!!.extend(newBuffer)
        newBuffer = source(max(size - buffer!!.remaining(), bufferSize)).second
      }
      buffer = buffer!!.extend(newBuffer)
    }
    return buffer!!.get(size).also {
      if (it.capacity() != it.limit()) buffer = Buffer.Empty
    }
  }

  suspend inline fun read(size: Int, acceptBuffer: (ByteBuffer) -> Unit) {
    acceptBuffer(read(size))
  }

  fun hasProbablyRemaining() = buffer != Buffer.Empty

  override fun toString(): String {
    return "StreamingByteBuffer(bufferSize=$bufferSize, buffer=$buffer)"
  }


  private sealed interface Buffer {

    fun remaining(): Int
    fun get(size: Int): ByteBuffer
    fun extend(newBuffer: ByteBuffer): Buffer

    object Empty : Buffer {
      override fun remaining(): Int = 0
      override fun get(size: Int): ByteBuffer = ByteBuffer.allocate(size).apply { limit(0) }
      override fun extend(newBuffer: ByteBuffer): Buffer = WholeBuffer(newBuffer)
      override fun toString(): String = "Empty"
    }

    class WholeBuffer(val buffer: ByteBuffer) : Buffer {
      override fun remaining(): Int = buffer.remaining()
      override fun get(size: Int): ByteBuffer =
        ByteArray(size).let {
          val readSize = Integer.min(size, buffer.remaining())
          buffer.get(it, 0, readSize)
          ByteBuffer.wrap(it, 0, readSize)
        }

      override fun extend(newBuffer: ByteBuffer): Buffer = DualBuffer(buffer, newBuffer)
      override fun toString(): String = """
        WholeBuffer(
          buffer=$buffer
          ${buffer.array().contentToString()}
        )""".trimMargin()
    }

    class DualBuffer(val first: ByteBuffer, val second: ByteBuffer) : Buffer {
      override fun remaining(): Int = first.remaining() + second.remaining()

      override fun get(size: Int): ByteBuffer {
        val firstSize = Integer.min(size, first.remaining())
        val secondSize = size - firstSize
        val firstBuffer = ByteArray(firstSize).let {
          val readSize = Integer.min(firstSize, second.remaining())
          first.get(it, 0, readSize)
          ByteBuffer.wrap(it, 0, readSize)
        }
        if (secondSize == 0) return firstBuffer
        val secondBuffer = ByteArray(secondSize).let {
          val readSize = Integer.min(secondSize, second.remaining())
          second.get(it, 0, readSize)
          ByteBuffer.wrap(it, 0, readSize)
        }
        return ByteBuffer.allocate(size).apply {
          put(firstBuffer)
          put(secondBuffer)
          flip()
        }
      }

      override fun extend(newBuffer: ByteBuffer): Buffer {
        if (first.remaining() == 0) {
          return if (second.remaining() == 0) WholeBuffer(newBuffer)
          else DualBuffer(second, newBuffer)
        }
        return MultiBuffer(first, second, newBuffer)
      }

      override fun toString(): String = """
        DualBuffer(
          first=$first
          ${first.array().contentToString()}
          second=$second
          ${second.array().contentToString()}
        )""".trimMargin()
    }

    class MultiBuffer(val buffers: List<ByteBuffer>) : Buffer {

      constructor(vararg buffers: ByteBuffer) : this(buffers.toList())

      override fun remaining(): Int = buffers.sumOf(ByteBuffer::remaining)

      override fun get(size: Int): ByteBuffer {
        var remaining = size
        val resultBuffer = ByteBuffer.allocate(size)
        for (buffer in buffers) {
          val readSize = Integer.min(remaining, buffer.remaining())
          repeat(readSize) {
            resultBuffer.put(buffer.get())
          }
          remaining -= readSize
          if (remaining == 0) break
        }
        resultBuffer.flip()
        return resultBuffer
      }

      override fun extend(newBuffer: ByteBuffer): Buffer {
        val rest = buffers.dropWhile { it.remaining() == 0 }
        return when (rest.size) {
          0 -> WholeBuffer(newBuffer)
          1 -> DualBuffer(rest.first(), newBuffer)
          else -> MultiBuffer(rest + newBuffer)
        }
      }

      override fun toString(): String = """
        MultiBuffer(
          ${
        buffers.joinToString(
          separator = "\n  ",
          prefix = "buffers=\n  ",
          postfix = "\n"
        ) {
          """
              $it
              ${it.array().contentToString()}
              """.trimIndent()
        }
      }
        )""".trimMargin()
    }
  }
}

context (ReadingByteBuffer) suspend fun byteOrNull() = read(1).let { if (it.remaining() != 1) null else it.get() }
context (ReadingByteBuffer) suspend fun byte() = read(1).get()
context (ReadingByteBuffer) suspend fun ubyteOrNull() = read(1).let { if (it.remaining() != 1) null else it.get().toUByte() }
context (ReadingByteBuffer) suspend fun ubyte() = byte().toUByte()

context (ReadingByteBuffer) suspend fun shortOrNull() = read(2).let { if (it.remaining() != 2) null else it.short }
context (ReadingByteBuffer) suspend fun short() = read(2).short
context (ReadingByteBuffer) suspend fun ushortOrNull() = read(2).let { if (it.remaining() != 2) null else it.short.toUShort() }
context (ReadingByteBuffer) suspend fun ushort() = short().toUShort()

context (ReadingByteBuffer) suspend fun intOrNull() = read(4).let { if (it.remaining() != 4) null else it.int }
context (ReadingByteBuffer) suspend fun int() = read(4).int
context (ReadingByteBuffer) suspend fun uintOrNull() = read(4).let { if (it.remaining() != 4) null else it.int.toUInt() }
context (ReadingByteBuffer) suspend fun uint() = int().toUInt()

context (ReadingByteBuffer) suspend fun longOrNull() = read(8).let { if (it.remaining() != 8) null else it.int }
context (ReadingByteBuffer) suspend fun long() = read(8).int
context (ReadingByteBuffer) suspend fun ulongOrNull() = read(8).let { if (it.remaining() != 8) null else it.int.toUInt() }
context (ReadingByteBuffer) suspend fun ulong() = long().toULong()
