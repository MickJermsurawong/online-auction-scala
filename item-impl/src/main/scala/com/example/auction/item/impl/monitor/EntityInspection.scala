package com.example.auction.item.impl.monitor

import java.util.concurrent.ConcurrentHashMap

import com.example.auction.item.api.monitor.PersistentEntityStateDto
import com.example.auction.item.impl.monitor.ces.InspectEntityStateCmd
import com.example.auction.item.impl.monitor.dao.EventDao
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntity, PersistentEntityRegistry}
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

trait EntityInspectionComponent {

  val entityInspectionService: EntityInspectionService
  lazy val inspectEntityCommandRegistry = new InspectEntityCommandRegistry()

}

class InspectEntityCommandRegistry {

  type InspectionHandler = PartialFunction[(PersistentEntityRegistry, String, InspectEntityStateCmd), Future[JsValue]]

  private val entityNameKeyCommandValue = new ConcurrentHashMap[String, InspectEntityStateCmd]()
  private val entityNameKeyInspectHandlerValue = new ConcurrentHashMap[String, InspectionHandler]()

  def getEntityNames: List[String] = {
    import scala.collection.JavaConversions._
    entityNameKeyCommandValue.keys().toList
  }

  def getInspectCommand(entityTypeName: String): InspectEntityStateCmd = {
    entityNameKeyCommandValue.get(entityTypeName)
  }

  def getInspectionHandler(entityTypeName: String): Option[InspectionHandler] = {
    Option(entityNameKeyInspectHandlerValue.get(entityTypeName))
  }

  def registerInspectionHandler(entityFactory: => PersistentEntity, cmd: InspectEntityStateCmd)(handler: InspectionHandler) = {

    val proto = entityFactory

    val entityTypeName = proto.entityTypeName
    entityNameKeyCommandValue.put(entityTypeName, cmd)
    entityNameKeyInspectHandlerValue.put(entityTypeName, handler)
  }
}

class EntityInspectionService(persistentEntityRegistry: PersistentEntityRegistry,
                              inspectionRegistry: InspectEntityCommandRegistry,
                              val cassandraSession: CassandraSession) extends EventDao {

  def inspectEntity(entityName: String, entityId: String,
                    from: Option[Long], to: Option[Long])(implicit ec: ExecutionContext): Future[PersistentEntityStateDto] = {
    for {
      currentState <- askCurrentState(entityName, entityId)
      events <- getEvents(entityName, entityId, from, to)
    } yield {
      PersistentEntityStateDto(currentState, events)
    }
  }

  def getEntityNames: Future[List[String]] = {
    Future.successful(inspectionRegistry.getEntityNames)
  }

  private def askCurrentState(entityTypeName: String, entityId: String): Future[JsValue] = {
    val cmd = getInspectCommand(entityTypeName)
    inspectionRegistry.getInspectionHandler(entityTypeName) match {
      case Some(handler) => handler(persistentEntityRegistry, entityId, cmd)
      case None => throw new NoSuchElementException(s"No inspection has been registered for entity type [$entityTypeName]")
    }
  }

  private def getInspectCommand(entityTypeName: String): InspectEntityStateCmd = {
    inspectionRegistry.getInspectCommand(entityTypeName)
  }

}
