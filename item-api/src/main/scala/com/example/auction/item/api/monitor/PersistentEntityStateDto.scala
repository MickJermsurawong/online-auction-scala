package com.example.auction.item.api.monitor

import java.time.Instant
import java.util.UUID

import play.api.libs.json.{Format, JsValue, Json}

case class PersistentEntityStateDto(currentState: JsValue, events: Seq[PersistentEventDto])

object PersistentEntityStateDto {
  implicit val format: Format[PersistentEntityStateDto] = Json.format
}

case class PersistentEventDto(
                               eventName: String,
                               persistentId: String,
                               partitionNumber: Long,
                               sequenceNumber: Long,
                               timeStamp: Instant,
                               offSet: UUID,
                               event: JsValue,
                               serializationManifest: String
                             )

object PersistentEventDto {
  implicit val format: Format[PersistentEventDto] = Json.format
}

