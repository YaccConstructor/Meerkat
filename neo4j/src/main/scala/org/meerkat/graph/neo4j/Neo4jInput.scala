package org.meerkat.graph.neo4j

import org.meerkat.graph.neo4j.Neo4jInput.Entity
import org.meerkat.input.Input
import org.neo4j.graphdb._

import scala.collection.JavaConverters._
import scala.language.{dynamics, implicitConversions}

class Neo4jInput(db: GraphDatabaseService) extends Input[Entity, Entity] {
  private val internalIdToDbId =
    db.getAllNodes.asScala
      .map(_.getId)
      .zipWithIndex
      .map(_.swap)
      .toMap

  private val dbIdToInternalId =
    internalIdToDbId.map(_.swap)

  override def nodesCount: Int =
    internalIdToDbId.size

  override def filterEdges(nodeId: Int,
                           predicate: Entity => Boolean,
                           outgoing: Boolean): Seq[(Entity, Int)] =
    db.getNodeById(internalIdToDbId(nodeId))
      .getRelationships(
        if (outgoing) Direction.OUTGOING else Direction.INCOMING)
      .asScala
      .collect {
        case r if predicate(Entity(r)) =>
          val endId =
            if (outgoing) r.getEndNodeId
            else r.getStartNodeId
          (Entity(r), dbIdToInternalId(endId))
      }
      .toSeq

  override def checkNode(nodeId: Int,
                         predicate: Entity => Boolean): Option[Entity] = {
    val property =
      Entity(db.getNodeById(internalIdToDbId(nodeId)))
    Some(property).filter(predicate)
  }
}

object Neo4jInput {

  class Entity(val entity: PropertyContainer) extends Dynamic {
    def label(): String = entity match {
      case node: Node =>
        node.getLabels.asScala.head.name()
      case relationship: Relationship =>
        relationship.getType.name()
      case _ => ""
    }

    def hasLabel(label: String): Boolean =
      entity.asInstanceOf[Node].hasLabel(Label.label(label))

    def selectDynamic[T](name: String): T =
      entity.getProperty(name).asInstanceOf[T]

    def hasProperty[T](name: String): Boolean =
      entity.hasProperty(name)

    def getProperty[T](name: String): T =
      entity.getProperty(name).asInstanceOf[T]

    override def toString: String = {
      s"Entity(${label()}," +
        entity.getAllProperties.asScala
          .map { case (k, v) => s"$k=$v" }
          .mkString("{", ",", "}") + ")"
    }
  }

  object Entity {
    def apply(entity: PropertyContainer): Entity = new Entity(entity)
  }

  implicit def toPredicate(predicate: String => Boolean): (Entity => Boolean) =
    (p: Entity) => predicate(p.label())

  implicit def toPredicate(label: String): (Entity => Boolean) =
    (p: Entity) => p.label() == label

  implicit def entityInputToStringInput(
      neo4jInput: Neo4jInput): Input[String, String] =
    new Input[String, String] {
      override def nodesCount: Int = neo4jInput.nodesCount

      override def filterEdges(nodeId: Int,
                               predicate: String => Boolean,
                               outgoing: Boolean): Seq[(String, Int)] =
        neo4jInput
          .filterEdges(nodeId, x => predicate(x.label()), outgoing)
          .map { case (e, i) => (e.label(), i) }

      override def checkNode(nodeId: Int,
                             predicate: String => Boolean): Option[String] =
        neo4jInput
          .checkNode(nodeId, x => predicate(x.label()))
          .map(_.label())
    }
}
