package com.example.auction.item.impl.monitor.dao

import java.time.Instant

import com.datastax.driver.core.Row
import com.example.auction.item.api.monitor.PersistentEventDto
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

trait EventDao {

  def cassandraSession: CassandraSession

  def getEvents(entityTypeName: String, entityId: String, from: Option[Long], to: Option[Long])(implicit ex: ExecutionContext): Future[Seq[PersistentEventDto]] = {

    val persistentId = makePersistentId(entityTypeName, entityId)

    val jZero = java.lang.Long.valueOf(0)

    val sequence: java.lang.Long = from match {
      case None => jZero
      case Some(s) => java.lang.Long.valueOf(s)
    }

    val result = to match {
      case None => cassandraSession.selectAll(
        EventDao.queryEvents,
        persistentId,
        jZero,
        sequence
      )
      case Some(s) => cassandraSession.selectAll(
        EventDao.queryEventsUpperBound,
        persistentId,
        jZero,
        sequence,
        java.lang.Long.valueOf(s)
      )
    }

    result.map(rows => rows.map(toEventDto))
  }


  def toEventDto(row: Row) = {

    val unixTimeStamp = row.getTime(EventDao.convertedUnixTimestampFieldName)

    val serializationManifest = row.getString(EventDao.serializationManifestFieldName)

    PersistentEventDto(
      row.getString(EventDao.serializationManifestFieldName).split('.').last,
      row.getString(EventDao.persistenceIdFieldName),
      row.getLong(EventDao.partitionNumberFieldName),
      row.getLong(EventDao.sequenceNumberFieldName),
      Instant.ofEpochMilli(unixTimeStamp),
      row.getUUID(EventDao.timestampUuidFieldName),
      Json.parse(row.getBytes(EventDao.eventFieldName).array()),
      serializationManifest
    )
  }

  private def makePersistentId(entityTypeName: String, entityId: String) = s"$entityTypeName|$entityId"

}

object EventDao {

  val persistenceIdFieldName = "persistence_id"
  val partitionNumberFieldName = "partition_nr"
  val sequenceNumberFieldName = "sequence_nr"
  val timestampUuidFieldName = "timestamp"
  val convertedUnixTimestampFieldName = s"system.toUnixTimestamp($timestampUuidFieldName)"
  val eventFieldName = "event"
  val serializationManifestFieldName = "ser_manifest"

  val fieldNames =
    s"""
       |$persistenceIdFieldName,
       |$partitionNumberFieldName,
       |$sequenceNumberFieldName,
       |$timestampUuidFieldName,
       |$eventFieldName,
       |$serializationManifestFieldName
    """.stripMargin

  def queryEvents =
    s"""
        SELECT $fieldNames, toUnixTimestamp($timestampUuidFieldName)
        FROM "messages"
        WHERE $persistenceIdFieldName = ?
        AND $partitionNumberFieldName = ?
        AND $sequenceNumberFieldName >= ?
      """.stripMargin

  def queryEventsUpperBound =
    s"""
       $queryEvents
       AND $sequenceNumberFieldName <= ?
      """.stripMargin

}