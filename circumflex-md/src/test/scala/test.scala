package ru.circumflex.md.test

import org.specs.runner.JUnit4
import org.specs.Specification
import java.io.File
import ru.circumflex.md.Markdown
import org.apache.commons.io.{IOUtils, FileUtils}
import org.apache.commons.lang.StringUtils
import org.specs.matcher.Matcher
import org.specs.specification.Example

class SpecsTest extends JUnit4(MarkdownSpec)

object MarkdownSpec extends Specification {

  val beFine = new Matcher[String] {
    def apply(name: => String) = {
      val textFile = new File(this.getClass.getResource("/" + name + ".text").toURI)
      val htmlFile = new File(this.getClass.getResource("/" + name + ".html").toURI)
      val text = Markdown(FileUtils.readFileToString(textFile, "UTF-8")).trim
      val html = FileUtils.readFileToString(htmlFile, "UTF-8").trim
      val diffIndex = StringUtils.indexOfDifference(text, html)
      val diff = StringUtils.difference(text, html)
      (diffIndex == -1,
          "\"" + name + "\" is fine",
          "\"" + name + "\" fails at " + diffIndex + ": " + StringUtils.abbreviate(diff, 32))
    }
  }

  def process = addToSusVerb("process")

  "MarkdownProcessor" should process {
    "Amps and angle encoding" in {
      "Amps and angle encoding" must beFine
    }
    "Auto links" in {
      "Auto links" must beFine
    }
    "Backslash escapes" in {
      "Backslash escapes" must beFine
    }
    "Blockquotes with code blocks" in {
      "Blockquotes with code blocks" must beFine
    }
    "Hard-wrapped paragraphs with list-like lines" in {
      "Hard-wrapped paragraphs with list-like lines" must beFine
    }
    "Horizontal rules" in {
      "Horizontal rules" must beFine
    }
    "Inline HTML (Advanced)" in {
      "Inline HTML (Advanced)" must beFine
    }
    "Inline HTML (Simple)" in {
      "Inline HTML (Simple)" must beFine
    }
    "Inline HTML comments" in {
      "Inline HTML comments" must beFine
    }
  }
}