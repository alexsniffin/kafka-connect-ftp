package com.eneco.trading.kafka.connect.ftp.source

import java.time.Instant
import java.util

import org.apache.kafka.connect.source.SourceTaskContext

import scala.collection.JavaConverters._
import scala.collection.mutable

// allows storage and retrieval of meta datas into connect framework
class ConnectFileMetaDataStore(c: SourceTaskContext) extends FileMetaDataStore with Logging {
  // connect offsets aren't directly committed, hence we'll cache them
  private val cache = mutable.Map[String, FileMetaData]()

  override def get(path: String): Option[FileMetaData] =
    cache.get(path).orElse(getFromStorage(path))

  override def set(path: String, fileMetaData: FileMetaData): Unit = {
    log.info(s"ConnectFileMetaDataStore set ${path}")
    cache.put(path, fileMetaData)
  }

  def getFromStorage(path: String): Option[FileMetaData] =
    c.offsetStorageReader().offset(Map("path" -> path).asJava) match {
      case null =>
        log.info(s"meta store storage HASN'T ${path}")
        None
      case o =>
        log.info(s"meta store storage has ${path}")
        Some(connectOffsetToFileMetas(path, o))
    }

  def fileMetasToConnectPartition(meta:FileMetaData): util.Map[String, String] = {
    Map("path" -> meta.attribs.path).asJava
  }

  def connectOffsetToFileMetas(path:String, o:AnyRef): FileMetaData = {
    val jm = o.asInstanceOf[java.util.Map[String, AnyRef]]
    FileMetaData(FileAttributes(path, jm.get("size").asInstanceOf[Long],
      Instant.ofEpochMilli(jm.get("timestamp").asInstanceOf[Long])
    ), jm.get("hash").asInstanceOf[String],
      Instant.ofEpochMilli(jm.get("firstfetched").asInstanceOf[Long]),
      Instant.ofEpochMilli(jm.get("lastmodified").asInstanceOf[Long]),
      Instant.ofEpochMilli(jm.get("lastinspected").asInstanceOf[Long])
    )
  }

  def fileMetasToConnectOffset(meta: FileMetaData) = {
    Map("size" -> meta.attribs.size,
      "timestamp" -> meta.attribs.timestamp.toEpochMilli,
      "hash" -> meta.hash,
      "firstfetched" -> meta.firstFetched.toEpochMilli,
      "lastmodified" -> meta.lastModified.toEpochMilli,
      "lastinspected" -> meta.lastInspected.toEpochMilli
    ).asJava
  }
}