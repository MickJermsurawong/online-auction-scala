package com.example.auction.item.impl.monitor.ces

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.JsValue

trait InspectEntityStateCmd extends ReplyType[JsValue]

