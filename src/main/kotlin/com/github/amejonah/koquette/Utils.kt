package com.github.amejonah.koquette

import java.math.BigInteger
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import javax.swing.UIManager.put

inline fun <T> listOfUntilNull(generator: () -> T?): List<T> {
  var temp: T? = generator()
  val list = mutableListOf<T>()
  while (temp != null) {
    list.add(temp)
    temp = generator()
  }
  return list.toList()
}

inline fun <T> listOfMapUntilNull(initial: T?, generator: (T) -> T?): List<T> {
  if (initial == null) return listOf()
  var temp: T? = generator(initial)
  val list = mutableListOf<T>(initial)
  while (temp != null) {
    list.add(temp)
    temp = generator(temp)
  }
  return list.toList()
}

inline fun <T> listOfTimes(i: Int, generator: () -> T): List<T> {
  val list = mutableListOf<T>()
  if (i < 1) return list
  for (counter in 0 until i) {
    list.add(generator())
  }
  return list
}

inline fun <T> listOfTimesIndexed(i: Int, generator: (Int) -> T): List<T> {
  val list = mutableListOf<T>()
  if (i < 1) return list
  for (counter in 0 until i) {
    list.add(generator(counter))
  }
  return list
}

inline fun <T> untilNull(generator: () -> T?) {
  var temp: T? = generator()
  while (temp != null) {
    temp = generator()
  }
}

inline fun <T> untilNull(initial: T?, generator: (T) -> T?) {
  if (initial == null) return
  var temp: T? = generator(initial)
  while (temp != null) {
    temp = generator(temp)
  }
}

fun Class<*>.isBoxedPrimitive() =
  javaClass == Int::class.java || javaClass == Long::class.java || javaClass == Short::class.java || javaClass == Byte::class.java
          || javaClass == Float::class.java || javaClass == Double::class.java || javaClass == Boolean::class.java || javaClass == Char::class.java


fun List<ByteBuffer>.merge(): ByteBuffer {
  val buffer = ByteBuffer.allocate(sumOf { it.capacity() })
  forEach { buffer.put(it) }
  buffer.flip()
  return buffer
}

inline fun <K, V> Map<K, V>.getOrPut(key: K, default: () -> V): V {
  return get(key) ?: default().also { put(key, it) }
}

fun <A, B> List<A>.allZip(other: List<B>, check: (A, B) -> Boolean = { a, b -> a == b }): Boolean {
  if (size != other.size) return false
  for (i in indices) {
    if (!check(this[i], other[i])) return false
  }
  return true
}

fun <A, B> Array<A>.allZip(other: Array<B>, check: (A, B) -> Boolean = { a, b -> a == b }): Boolean {
  if (size != other.size) return false
  for (i in indices) {
    if (!check(this[i], other[i])) return false
  }
  return true
}

fun <A, B> Iterable<A>.allZip(other: Iterable<B>, check: (A, B) -> Boolean = { a, b -> a == b }): Boolean {
  val iterator1 = this.iterator()
  val iterator2 = other.iterator()
  while (iterator1.hasNext() && iterator2.hasNext()) {
    if (!check(iterator1.next(), iterator2.next())) return false
  }
  return !iterator1.hasNext() && !iterator2.hasNext()
}


fun AsynchronousServerSocketChannel.bind(port: Int = 0) = bind(InetSocketAddress("localhost", port))
