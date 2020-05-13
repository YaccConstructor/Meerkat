package org.meerkat.graph.benchmark.neosemantics

import java.io.File
import java.util

import org.meerkat.Syntax.syn
import org.meerkat.graph.neo4j.Neo4jInput
import org.meerkat.graph.neo4j.Neo4jInput.Entity
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.neo4j.graphdb.factory.{GraphDatabaseFactory, GraphDatabaseSettings}

import scala.Option

trait WithNeo4j extends App with SimpleBenchmark {
  def withGraph(pathToFile: String, pathToConf: String)(
      fun: (Neo4jInput => Unit)): Unit = {

    val dbFile = new File(pathToFile)
    val dbConf = new File(pathToConf)
    val graphDb = new GraphDatabaseFactory()
      .newEmbeddedDatabaseBuilder(dbFile)
      .loadPropertiesFromFile(dbConf.getAbsolutePath)
      .setConfig(GraphDatabaseSettings.allow_upgrade, "true")
      .newGraphDatabase()

    val tx    = graphDb.beginTx()
    val graph = new Neo4jInput(graphDb)
    try {
      fun(graph)
    } finally {
      tx.success()
      tx.close()
    }
  }

  def printPathLengthWithStartAndFinish(
      testResult: Iterable[(String, (Double, Double, List[(Int, String)]))])
    : Unit = {
    testResult
    //      .take(5)
      .map({
        case (startUri, (time, mem, res)) =>
          (startUri, time, mem, res.map(_._1).sum, res.length, res)
      })
      .foreach({
        case (startUri, time, mem, sum, len, res) =>
          print(s"$startUri $time $mem $sum $len ")
          res.foreach(p => print(s"$p "))
          println()
      })
  }

  def benchmarkQueryToDb(queryToDb: QueryToDb[Entity, Entity, String])(
      graph: Neo4jInput) =
    benchmarkSample(graph,
                    queryToDb.findPathLengthWithFinishQuery,
                    queryToDb.startVertexes)
}

object RdfConstants {
  val RDFS__SUB_CLASS_OF       = "rdfs__subClassOf"
  val RDF__TYPE                = "rdf__type"
  val SKOS__BROADER_TRANSITIVE = "skos__broaderTransitive"
  val SKOS__NARROWER_TRANSITIVEY = "skos__narrowerTransitive"
  val OWL__ON_PROPERTY = "owl__onProperty"
}

sealed trait QueryToDb[L, N, V] {
  def checkIfHas[T](e: Entity, prop: String)(p: T => Boolean): Boolean = {
    e.hasProperty(prop) && p(e.getProperty[T](prop))
  }

  def startVertexes: Symbol[L, Entity, Entity]
  def findFinishQuery: Symbol[L, N, V]
  def findPathsQuery: Symbol[L, N, util.ArrayDeque[V]]
  def findPathLengthQuery: Symbol[L, N, Int]
  def findPathLengthWithFinishQuery: Symbol[L, N, (Int, V)]
}

sealed trait RdfQuery extends QueryToDb[Entity, Entity, String] {
  protected val uriFromV: Symbol[Entity, Entity, String] =
    syn(V((_: Entity) => true) ^ ((_: Entity).getProperty[String]("uri")))

  override def findPathLengthWithFinishQuery
    : Symbol[Entity, Entity, (Int, String)] =
    syn(findPathLengthQuery ~ uriFromV & { case len ~ uri => (len, uri) })

  override def startVertexes: Symbol[Entity, Entity, Entity] =
    syn(V(checkIfHas[String](_: Entity, "uri")(_ => true)) ^^)
}

case class SameGeneration(private val edgeName: String) extends RdfQuery {
  protected val OUT: Edge[Entity] = outE((_: Entity).label() == edgeName)
  protected val IN: Edge[Entity]  = inE((_: Entity).label() == edgeName)

//  override def startVertexes: Symbol[Entity, Entity, Entity] =
//    syn(OUT ~ OUT ~ OUT ~ OUT ~ super.startVertexes ~ OUT &&)

  private def S: Symbol[Entity, Entity, _] = syn(IN ~ S ~ OUT | OUT)
  override def findFinishQuery: Symbol[Entity, Entity, String] =
    syn(
      S.?
      // without & {case ...} type error
        ~ uriFromV
        & { case _ ~ a => a }
    )

  override def findPathsQuery: Symbol[Entity, Entity, util.ArrayDeque[String]] =
    syn(
      syn(
        IN ~ uriFromV ~ findPathsQuery ~ OUT ~ uriFromV
          & {
            case (l: String) ~ (deque: util.ArrayDeque[String]) ~ (r: String) =>
              deque.addLast(r)
              deque.addFirst(l)
              deque
          }
          | OUT ~ uriFromV
            & {
              case uri: String =>
                val deque = new util.ArrayDeque[String]()
                deque.add(uri)
                deque
            }
      )
    )
  override def findPathLengthQuery: Symbol[Entity, Entity, Int] =
    syn(IN ~ findPathLengthQuery ~ OUT & {
      case inLen: Int => inLen + 2
    } | OUT ~ uriFromV & (_ => 1))

}

case object GPPerf1 extends QueryToDb[Entity, Entity, String] with RdfQuery {

  protected val SCO: Edge[Entity] = outE(
    (_: Entity).label() == RdfConstants.RDFS__SUB_CLASS_OF)
  protected val SCOR: Edge[Entity] = inE(
    (_: Entity).label() == RdfConstants.RDFS__SUB_CLASS_OF)

  protected val T: Edge[Entity] = outE(
    (_: Entity).label() == RdfConstants.RDF__TYPE)
  protected val TR: Edge[Entity] = inE(
    (_: Entity).label() == RdfConstants.RDF__TYPE)

  private def S: Symbol[Entity, Entity, _] =
    syn(SCOR ~ S ~ SCO | TR ~ S ~ T | SCOR ~ SCO | TR ~ T)

  override def findFinishQuery: Symbol[Entity, Entity, String] =
    syn(
      S.?
      // without & {case ...} type error
        ~ syn(V((_: Entity) => true) ^ ((_: Entity).getProperty[String]("uri")))
        & { case _ ~ a => a }
    )
  override def findPathsQuery: Symbol[Entity, Entity, util.ArrayDeque[String]] =
    ???

  override def findPathLengthQuery: Symbol[Entity, Entity, Int] =
    syn(
      SCOR ~ findPathLengthQuery ~ SCO & {
        case inLen: Int => inLen + 2
      }
        | TR ~ findPathLengthQuery ~ T & {
          case inLen: Int => inLen + 2
        }
        | SCOR ~ uriFromV ~ SCO & (_ => 2)
        | TR ~ uriFromV ~ T & (_ => 2)
    )
}

object SameGenerationExample {

  val uriV: Symbol[Entity, Entity, Entity] =
    syn(V((_: Entity).hasProperty("uri")) ^^)

  def queryFromV[L, N](startV: Symbol[L, N, _],
                               query: Symbol[L, N, _]) =
    syn(startV ~ query ~ uriV &
        {case _ ~ (len:Int) ~ (v:Entity) =>
             (len, v.getProperty[String]("uri") )})


  def queryFromV1[L, N](startV: Symbol[L, N, _],
                       query: Symbol[L, N, _]) =
    syn(startV ~ query ~ uriV & {case _ ~ _ ~ (v:Entity) => v.getProperty[String]("uri")})


  def reduceChoice[L, N, V](xs: List[Symbol[L, N, V]]): Symbol[L, N, V] = {
    xs match {
      case x :: Nil => x
      case x :: y :: xs =>
        syn(xs.foldLeft(x | y)(_ | _))
    }
  }

  def sameGen[L, N, V](
      brs: List[(Symbol[L, N, V], Symbol[L, N, V])]): Symbol[L, N, Int] =
    reduceChoice(
      brs.map {
        case (lbr, rbr) =>
          syn((lbr ~ (sameGen(brs).?) ~ rbr) & {
            case _~((x:Int)::Nil)~_ =>  x + 2
            case _~Nil~_ => 2
          })
      }
    )

  def sameGen1[L, N, V]( brs: List[(Symbol[L, N, V], Symbol[L, N, V])]):Symbol[L,N,_] =
    reduceChoice(
      brs.map {
        case (lbr, rbr) => syn(lbr ~ (sameGen1(brs).?) ~ rbr)
      }
    )

  def sameGen2():Symbol[Entity,Entity,_] =
    syn(inE((_: Entity).label() == "skos__narrowerTransitive") ~ sameGen2 .? ~ (outE((_: Entity).label() == "skos__narrowerTransitive")))


}
