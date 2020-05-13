package org.meerkat.graph.benchmark

import org.meerkat.graph.benchmark.neosemantics.{GPPerf1, RdfConstants, RdfQuery, SameGeneration, WithNeo4j}
import org.meerkat.graph.neo4j.Neo4jInput.Entity
import org.meerkat.input.Input
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers.Parsers
import org.meerkat.parsers._
import org.meerkat.Syntax.syn
import org.meerkat.graph.benchmark.neosemantics.RdfConstants._
import org.meerkat.graph.benchmark.neosemantics.SameGenerationExample._
import org.neo4j.cypher.internal.v3_4.expressions.True

object Main extends App with WithNeo4j {
  if (args.length < 3) {
    println("args.len < 3")
    System.exit(1)
  }
  //  val pathToFile = "/home/mishock/neo4j/data/databases/graph.db"
  val pathToFile = args(0)
//  val pathToConf = "/home/mishock/neo4j/conf/neo4j.conf"
  val pathToConf = args(1)

  val experimentType = args(2)

  def runWithGraph =
    withGraph(
      pathToFile,
      pathToConf
    )(_)

  def runQTB(tp: RdfQuery) =
    runWithGraph(
      { graph =>
        printPathLengthWithStartAndFinish(
          benchmarkQueryToDb(SameGeneration(RDFS__SUB_CLASS_OF))(
            graph))
      }
    )

  def runExample2(brs: List[String]) =
    runWithGraph({ graph: Input[Entity, Entity] =>
      val symbolBrs = brs
        .map(name =>
          (syn(inE((_: Entity).label() == name) ^^), syn(outE((_: Entity).label() == name)^^)))
        .toList

      val q = queryFromV(syn(V(getIdFromNode(_: Entity) == 1)^^),
                         sameGen(symbolBrs))

      val result = executeQuery(q, graph).toList


      print(result.map(_._1).sum)
    })

  def runExample(brs: List[String]) =
    runWithGraph({ graph: Input[Entity, Entity] =>
      val symbolBrs = brs
        .map(name =>
          (syn(inE((_: Entity).label() == name) ^^), syn(outE((_: Entity).label() == name)^^)))
        .toList


      val result =
        benchmarkExample(graph, sameGen(symbolBrs), uriV)

      printPathLengthWithStartAndFinish(
        result
      )
    })

  def runProfiler(brs: List[String]) =
    runWithGraph({ graph: Input[Entity, Entity] =>
      val symbolBrs = brs
        .map(name =>
          (syn(inE((_: Entity).label() == name) ^^), syn(outE((_: Entity).label() == name) ^^)))

      val start_uri_enzime = Set(
        "http://purl.uniprot.org/enzyme/1.1.1.271",
        "http://purl.uniprot.org/enzyme/6.1.2.1",
        "http://purl.uniprot.org/enzyme/5.99.1.1",
        "http://purl.uniprot.org/enzyme/5.99.1.4",
        "http://purl.uniprot.org/enzyme/6.3.4.11",
        "http://purl.uniprot.org/enzyme/6.3.4.10",
        "http://purl.uniprot.org/enzyme/6.3.4.13",
        "http://purl.uniprot.org/enzyme/6.3.4.12",
        "http://purl.uniprot.org/enzyme/6.3.4.22",
        "http://purl.uniprot.org/enzyme/6.3.4.24",
        "http://purl.uniprot.org/enzyme/6.3.4.23",
        "http://purl.uniprot.org/enzyme/6.3.4.20",
        "http://purl.uniprot.org/enzyme/6.3.4.19",
        "http://purl.uniprot.org/enzyme/6.3.4.18",
        "http://purl.uniprot.org/enzyme/5.4.4.5",
        "http://purl.uniprot.org/enzyme/5.4.4.8",
        "http://purl.uniprot.org/enzyme/5.4.4.7",
        "http://purl.uniprot.org/enzyme/6.3.4.15",
        "http://purl.uniprot.org/enzyme/6.3.4.14",
        "http://purl.uniprot.org/enzyme/6.3.4.17",
        "http://purl.uniprot.org/enzyme/5.4.4.2",
        "http://purl.uniprot.org/enzyme/5.4.4.1",
        "http://purl.uniprot.org/enzyme/5.4.4.4",
        "http://purl.uniprot.org/enzyme/5.4.4.3",
        "http://purl.uniprot.org/enzyme/5.4.2.10",
        "http://purl.uniprot.org/enzyme/5.3.2.7",
        "http://purl.uniprot.org/enzyme/5.3.2.6",
        "http://purl.uniprot.org/enzyme/5.3.2.4",
        "http://purl.uniprot.org/enzyme/5.3.2.3",
        "http://purl.uniprot.org/enzyme/5.3.2.2",
        "http://purl.uniprot.org/enzyme/5.3.2.1",
        "http://purl.uniprot.org/enzyme/5.3.2.8",
        "http://purl.uniprot.org/enzyme/7.6.2.16",
        "http://purl.uniprot.org/enzyme/5.3.99.10",
        "http://purl.uniprot.org/enzyme/5.4.2.13",
        "http://purl.uniprot.org/enzyme/7.5.2.10",
        "http://purl.uniprot.org/enzyme/7.5.2.13",
        "http://purl.uniprot.org/enzyme/7.5.2.12",
        "http://purl.uniprot.org/enzyme/7.5.2.11",
        "http://purl.uniprot.org/enzyme/5.1.3.10",
        "http://purl.uniprot.org/enzyme/5.1.3.11",
        "http://purl.uniprot.org/enzyme/5.1.3.12",
        "http://purl.uniprot.org/enzyme/5.1.3.13",
        "http://purl.uniprot.org/enzyme/5.1.3.14",
        "http://purl.uniprot.org/enzyme/5.1.3.15",
        "http://purl.uniprot.org/enzyme/5.1.3.16",
        "http://purl.uniprot.org/enzyme/5.1.3.17",
        "http://purl.uniprot.org/enzyme/5.1.3.18",
        "http://purl.uniprot.org/enzyme/5.1.3.19",
        "http://purl.uniprot.org/enzyme/5.1.3.20"
      )

      val start_uri_geo = Set("http://lod.geospecies.org/ses/wprja",
        "http://lod.geospecies.org/ses/cFKuH",
        "http://lod.geospecies.org/ses/O7X2b",
        "http://lod.geospecies.org/ses/Sk5AV",
        "http://lod.geospecies.org/ses/eN6ZP",
        "http://lod.geospecies.org/ses/CjtGp",
        "http://lod.geospecies.org/ses/U5MXl",
        "http://lod.geospecies.org/ses/Q8pGJ",
        "http://lod.geospecies.org/ses/dvQtf",
        "http://lod.geospecies.org/ses/wzNEU",
        "http://lod.geospecies.org/ses/k8voD",
        "http://lod.geospecies.org/ses/bfNx2",
        "http://lod.geospecies.org/ses/69rGX",
        "http://lod.geospecies.org/ses/hS2mo",
        "http://lod.geospecies.org/ses/U2g5U",
        "http://lod.geospecies.org/ses/MDjHP",
        "http://lod.geospecies.org/ses/RcfUk",
        "http://lod.geospecies.org/ses/iGmhm",
        "http://lod.geospecies.org/ses/WdJNT",
        "http://lod.geospecies.org/ses/h9IFM",
        "http://lod.geospecies.org/ses/Cob3k",
        "http://lod.geospecies.org/ses/otmVK",
        "http://lod.geospecies.org/ses/9czcr",
        "http://lod.geospecies.org/ses/ptfqF",
        "http://lod.geospecies.org/ses/lLHZA",
        "http://lod.geospecies.org/ses/B23IY",
        "http://lod.geospecies.org/ses/UaN8h",
        "http://lod.geospecies.org/ses/3ghcM",
        "http://lod.geospecies.org/ses/BoDyd",
        "http://lod.geospecies.org/ses/H3wFQ",
        "http://lod.geospecies.org/ses/GrQWJ",
        "http://lod.geospecies.org/ses/On4mB",
        "http://lod.geospecies.org/ses/zfcUq",
        "http://lod.geospecies.org/ses/96UbQ",
        "http://lod.geospecies.org/ses/iKXV3",
        "http://lod.geospecies.org/ses/w8Mlk",
        "http://lod.geospecies.org/ses/bKahy",
        "http://lod.geospecies.org/ses/pjjOZ",
        "http://lod.geospecies.org/ses/Ha8Zt",
        "http://lod.geospecies.org/ses/RSxoH",
        "http://lod.geospecies.org/ses/J6R3L",
        "http://lod.geospecies.org/ses/j6USJ",
        "http://lod.geospecies.org/ses/JjGfC",
        "http://lod.geospecies.org/ses/PANBa",
        "http://lod.geospecies.org/ses/FFXMJ",
        "http://lod.geospecies.org/ses/mnh2J",
        "http://lod.geospecies.org/ses/OMyPh",
        "http://lod.geospecies.org/ses/LzJcs",
        "http://lod.geospecies.org/ses/mngPk",
        "http://lod.geospecies.org/ses/PBrhI"
      )
      val start : Symbol[Entity, Entity, Entity] = syn(V((v: Entity) => v.hasProperty("uri") && start_uri_geo(v.getProperty[String]("uri")) )  ^^)
      val result =
        benchmarkExample(graph, sameGen(symbolBrs), start)

      printPathLengthWithStartAndFinish(
        result
      )
    })
  def runExample1(brs: List[String]) =
    runWithGraph({ graph: Input[Entity, Entity] =>
      val symbolBrs = brs
        .map(name =>
          (syn(inE((_: Entity).label() == name) ^^), syn(outE((_: Entity).label() == name)^^)))
        .toList


      val result =
        benchmarkExample1(graph, sameGen2(), uriV)

      print(result.take(100).toList)
    })

  experimentType match {
    case "GPPerf1" => runQTB(GPPerf1)
    case "GPPerf2" => runQTB(SameGeneration(RDFS__SUB_CLASS_OF))
    case "GoProp"  => runQTB(SameGeneration(OWL__ON_PROPERTY))
    case "enzime"  => runQTB(SameGeneration(SKOS__NARROWER_TRANSITIVEY))
    case "GeoBorderTr" =>
      runQTB(SameGeneration(SKOS__BROADER_TRANSITIVE))

    case "type_and_subClass" => runExample(RDFS__SUB_CLASS_OF :: RDF__TYPE :: Nil)
    case "subClass" => runExample(RDFS__SUB_CLASS_OF :: Nil)
    case "NT" => runExample(SKOS__NARROWER_TRANSITIVEY :: Nil)
    case "BT" => runExample(SKOS__BROADER_TRANSITIVE :: Nil)

    case "example" => runExample(RDFS__SUB_CLASS_OF :: Nil)
    case "profiler" => runProfiler(SKOS__NARROWER_TRANSITIVEY :: Nil)
    case _ => throw new RuntimeException("unknown experiment type")
  }
}
