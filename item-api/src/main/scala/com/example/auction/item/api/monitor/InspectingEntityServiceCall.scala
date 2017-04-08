package com.example.auction.item.api.monitor

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.Method

trait InspectingEntityServiceCall {

  def getEntityNames(): ServiceCall[NotUsed, Seq[String]]

  def inspectEntity(entityName: String, entityId: String, from: Option[Long], to: Option[Long]): ServiceCall[NotUsed, PersistentEntityStateDto]

  def restCallGetEntityNames(name: String) = {
    restCall(Method.GET, s"/api/internal/$name/inspect/entityNames", getEntityNames _)
  }

  def restCallInspectEntity(name: String) = {
    restCall(Method.GET, s"/api/internal/$name/inspect/entities/:entityName/:entityId?from&to", inspectEntity _)
  }
}
