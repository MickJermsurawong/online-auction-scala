package com.example.auction.item.impl.monitor.ces

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import play.api.libs.json.{JsValue, Json, Writes}

import scala.reflect.ClassTag

trait InspectableEntity extends PersistentEntity {

  protected def inspection[C <: Command with InspectEntityStateCmd : ClassTag]
  (behavior: Behavior)(implicit tjs: Writes[State]) = {

    state: State =>
      Actions().onReadOnlyCommand[C, JsValue] {
        case (_: C, ctx, state) => ctx.reply(Json.toJson(state))
      }.orElse(behavior(state))
  }

}