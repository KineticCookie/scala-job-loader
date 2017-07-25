package io.hydrosphere.serving.runtime.scala

import java.io.File
import scala.reflect.runtime.universe

/**
  * Created by bulat on 20/07/2017.
  */

object Main extends App {
  val INPUT = List(
    Map(
      "name" -> "Igor",
      "surname" -> "Igorevich",
      "address" -> "Russia"
    ).asInstanceOf[Map[String, Any]],
    Map (
      "name" -> "Vasya",
      "surname" -> "Vasyavich",
      "address" -> "Kazan"
    )
  )

  val file = new File("example/target/scala-2.11/example.jar")
  val job = JobInstance.fromJar(file)
  val result = job.run(INPUT)
  println(result)
}
