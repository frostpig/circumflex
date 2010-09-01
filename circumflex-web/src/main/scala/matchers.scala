package ru.circumflex.web

import util.matching.Regex
import scala.collection.immutable.Map
import collection.Iterator

/*#! Matchers

The `Matcher` trait and the `MatchResult` class are the cornerstone of request routing.

Matchers define mechanisms which perform request matching. They yield zero or more
match results on successfull match and are used in routes definition.

Match results are subsequently used inside matched route's block.

TODO add more documentation and samples on how to use match results
*/
class MatchResult(val name: String,
            val params: (String, String)*) extends Map[String, String] {
  def +[B1 >: String](kv: (String, B1)): Map[String, B1] = this
  def -(key: String): Map[String, String] = this
  def iterator: Iterator[(String, String)] = params.iterator

  def get(index: Int): Option[String] =
    if (params.indices.contains(index)) Some(params(index)._2)
    else None
  def get(name: String): Option[String] = params.find(_._1 == name) match {
    case Some(param: Pair[String, String]) => Some(param._2)
    case _ => None
  }

  def apply(): String = apply(0)
  def apply(index: Int): String = get(index).getOrElse("")

  def splat: Seq[String] = params.filter(_._1 == "splat").map(_._2).toSeq

  override def default(key: String): String = ""
  override def toString = apply(0)
}

/*! Matchers can be composed together using the `&` method. The `CompositeMatcher` will
only yield match results if all it's matchers succeed.*/
trait Matcher {
  def apply(): Option[Seq[MatchResult]]
  def add(matcher: Matcher): CompositeMatcher
  def &(matcher: Matcher) = add(matcher)
}

trait AtomicMatcher extends Matcher {
  def name: String
  def add(matcher: Matcher) = new CompositeMatcher()
      .add(this)
      .add(matcher)
}

class CompositeMatcher extends Matcher {
  private var _matchers: Seq[Matcher] = Nil
  def matchers = _matchers
  def add(matcher: Matcher): CompositeMatcher = {
    _matchers ++= List(matcher)
    return this
  }
  def apply() = try {
    val matches = _matchers.flatMap(m => m.apply match {
      case Some(matches: Seq[MatchResult]) => matches
      case _ => throw new MatchError
    })
    if (matches.size > 0) Some(matches)
    else None
  } catch {
    case e: MatchError => None
  }
}

/*! TODO document the `RegexMatcher` */
class RegexMatcher(val name: String,
                   val value: String,
                   protected var regex: Regex,
                   protected var groupNames: Seq[String] = Nil) extends AtomicMatcher {
  def this(name: String, value: String, pattern: String) = {
    this(name, value, null, Nil)
    processPattern(pattern)
  }
  protected def processPattern(pattern: String): Unit = {
    this.groupNames = List("splat")    // for `group(0)`
    this.regex = (""":\w+|[\*+.()]""".r.replaceAllIn(pattern, m => m.group(0) match {
      case "*" | "+" =>
        groupNames ++= List("splat")
        "(." + m.group(0) + "?)"
      case "." | "(" | ")" =>
        "\\\\" + m.group(0)
      case _ =>
        groupNames ++= List(m.group(0).substring(1))
        "([^/?&#.]+)"
    })).r
  }
  def groupName(index: Int): String=
    if (groupNames.indices.contains(index)) groupNames(index)
    else "splat"
  def apply(): Option[Seq[MatchResult]] = {
    val m = regex.pattern.matcher(value)
    if (m.matches) {
      val matches = for (i <- 0 to m.groupCount) yield groupName(i) -> m.group(i)
      Some(List(new MatchResult(name, matches: _*)))
    } else None
  }
}

/*! TODO document the headers matcher */
class HeaderMatcher(name: String,
                    regex: Regex,
                    groupNames: Seq[String] = Nil)
    extends RegexMatcher(name, request.headers.getOrElse(name,""), regex, groupNames) {
  def this(name: String, pattern: String) = {
    this(name, null, Nil)
    processPattern(pattern)
  }
}

class HeaderMatcherHelper(name: String) {
  def apply(regex: Regex, groupNames: Seq[String] = Nil) = 
    new HeaderMatcher(name, regex, groupNames)
  def apply(pattern: String) = new HeaderMatcher(name, pattern)
}