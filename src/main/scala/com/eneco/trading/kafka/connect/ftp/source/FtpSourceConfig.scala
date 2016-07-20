package com.eneco.trading.kafka.connect.ftp.source

import java.util

import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{AbstractConfig, ConfigDef}
import scala.collection.JavaConverters._

case class MonitorConfig(topic:String, path:String, tail:Boolean)

object FtpSourceConfig {
  val Address = "ftp.address"
  val User = "ftp.user"
  val Password = "ftp.password"
  val RefreshRate = "ftp.refresh"
  val MonitorTail = "ftp.monitor.tail"
  val MonitorUpdate = "ftp.monitor.update"
  val FileMaxAge = "ftp.file.maxage"

  val definition: ConfigDef = new ConfigDef()
    .define(Address, Type.STRING, Importance.HIGH, "ftp address")
    .define(User, Type.STRING, Importance.HIGH, "ftp user name to login")
    .define(Password, Type.PASSWORD, Importance.HIGH, "ftp password to login")
    .define(RefreshRate, Type.STRING, Importance.HIGH, "how often the ftp server is polled; ISO8601 duration")
    .define(FileMaxAge, Type.STRING, Importance.HIGH, "ignore files older than this; ISO8601 duration")
    .define(MonitorTail, Type.LIST, Importance.HIGH, "comma separated lists of path:destinationtopic; tail of file is tracked")
    .define(MonitorUpdate, Type.LIST, Importance.HIGH, "comma separated lists of path:destinationtopic; whole file is tracked")
}

class FtpSourceConfig(props: util.Map[String, String])
  extends AbstractConfig(FtpSourceConfig.definition, props) {

  def ftpMonitorConfigs(): Seq[MonitorConfig] = {
    lazy val topicPathRegex = "([^:]*):(.*)".r
    getList(FtpSourceConfig.MonitorTail).asScala.map { case topicPathRegex(path, topic) => MonitorConfig(topic,path,true) } ++
    getList(FtpSourceConfig.MonitorUpdate).asScala.map { case topicPathRegex(path, topic) => MonitorConfig(topic,path,false) }
  }
}