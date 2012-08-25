/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka

import metrics.{KafkaMetricsReporterMBean, KafkaMetricsReporter, KafkaMetricsConfig}
import server.{KafkaConfig, KafkaServerStartable, KafkaServer}
import utils.{Utils, Logging}

object Kafka extends Logging {

  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      println("USAGE: java [options] %s server.properties".format(classOf[KafkaServer].getSimpleName()))
      System.exit(1)
    }
  
    try {
      val props = Utils.loadProps(args(0))
      val serverConfig = new KafkaConfig(props)
      val metricsConfig = new KafkaMetricsConfig(props)
      metricsConfig.reporters.foreach(reporterType => {
        val reporter = Utils.getObject[KafkaMetricsReporter](reporterType)
        reporter.init(props)
        if (reporter.isInstanceOf[KafkaMetricsReporterMBean])
          Utils.registerMBean(reporter, reporter.asInstanceOf[KafkaMetricsReporterMBean].getMBeanName)
      })

      val kafkaServerStartble = new KafkaServerStartable(serverConfig)

      // attach shutdown handler to catch control-c
      Runtime.getRuntime().addShutdownHook(new Thread() {
        override def run() = {
          kafkaServerStartble.shutdown
        }
      });

      kafkaServerStartble.startup
      kafkaServerStartble.awaitShutdown
    }
    catch {
      case e => fatal(e)
    }
    System.exit(0)
  }
}
