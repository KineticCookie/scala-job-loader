package io.hydrosphere.serving.dummy

import io.hydrosphere.serving.runtime.scala.lib.ServingJob

/**
  * Created by bulat on 24/07/2017.
  */
object DummyJob extends ServingJob[DummyInput, DummyOutput]{
  override def execute(data: List[DummyInput]): List[DummyOutput] = {
    data.map(x => DummyOutput(s"${x.name} ${x.surname}"))
  }
}
