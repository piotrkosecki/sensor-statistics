package com.piotrkosecki.sensorstatistics.utils
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

import scala.annotation.tailrec

object ImplicitUtilsSpecification extends Properties("ImplicitUtils") {

  @tailrec
  def compare(list: List[Int])(op: (Int, Int) => Boolean): Boolean = list match {
    case Nil => true
    case _ :: Nil => true
    case head :: second :: tail => op(head, second) && compare(second :: tail)(op)
  }
  import ImplicitUtils._

  property("sorted") = forAll { list: List[Option[Int]] =>
    val (opts, nonOpts) = list.sorted.zipWithIndex.partition(_._1.isDefined)
    opts.map(_._2).maxOption.getOrElse(-1) < nonOpts.map(_._2).minOption.getOrElse(Int.MaxValue) &&
    compare(opts.map(_._1.get))(_ >= _)
  }
}
