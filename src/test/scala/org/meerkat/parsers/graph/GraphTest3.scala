package org.meerkat.parsers.graph

import org.meerkat.Syntax._
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.meerkat.util.IGraph
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.OptionValues._

import scalax.collection.Graph
import scalax.collection.edge.Implicits._

class GraphTest3 extends FunSuite {
  val E: Nonterminal = syn(
    "(" ~~ E ~~ ")" |
      epsilon)
  val g = Graph(
    (0 ~+#> 1) ("("),
    (1 ~+#> 2) ("("),
    (2 ~+#> 0) ("("),
    (0 ~+#> 3) (")"),
    (3 ~+#> 0) (")")
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(E, IGraph(g)).value shouldBe SPPFStatistics(8, 14, 8, 23, 1)
  }
}
