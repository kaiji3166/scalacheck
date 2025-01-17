/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck.util

import org.scalacheck.ScalaVersionSpecific._

sealed trait FreqMap[T] extends Serializable {
  protected val underlying: scala.collection.immutable.Map[T,Int]
  def total: Int

  def +(t: T): FreqMap[T] = new FreqMap[T] {
    private val n = FreqMap.this.underlying.get(t) match {
      case None => 1
      case Some(n) => n+1
    }
    val underlying = FreqMap.this.underlying + (t -> n)
    val total = FreqMap.this.total + 1
  }

  def -(t: T): FreqMap[T] = new FreqMap[T] {
    val underlying = FreqMap.this.underlying.get(t) match {
      case None => FreqMap.this.underlying
      case Some(n) => FreqMap.this.underlying + (t -> (n-1))
    }
    val total = FreqMap.this.total + 1
  }

  def ++(fm: FreqMap[T]): FreqMap[T] = new FreqMap[T] {
    private val keys = FreqMap.this.underlying.keySet ++ fm.underlying.keySet
    private val mappings = toLazyList(keys).map { x =>
      (x, fm.getCount(x).getOrElse(0) + FreqMap.this.getCount(x).getOrElse(0))
    }
    val underlying = scala.collection.immutable.Map(mappings: _*)
    val total = FreqMap.this.total + fm.total
  }

  def --(fm: FreqMap[T]): FreqMap[T] = new FreqMap[T] {
    val underlying = FreqMap.this.underlying transform {
      case (x,n) => n - fm.getCount(x).getOrElse(0)
    }
    lazy val total = underlying.valuesIterator.foldLeft(0)(_ + _)
  }

  def getCount(t: T) = underlying.get(t)

  def getCounts: List[(T,Int)] = underlying.toList.sortBy(-_._2)

  def getRatio(t: T) = for(c <- getCount(t)) yield c.toFloat/total

  def getRatios = for((t,c) <- getCounts) yield (t, c.toFloat/total)

  override def toString = underlying.toString
}

object FreqMap {
  def empty[T]: FreqMap[T] = new FreqMap[T] {
    val underlying = scala.collection.immutable.Map.empty[T,Int]
    val total = 0
  }
}
