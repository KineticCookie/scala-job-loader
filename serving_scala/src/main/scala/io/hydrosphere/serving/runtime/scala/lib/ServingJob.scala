package io.hydrosphere.serving.runtime.scala.lib

/**
  * Trait that indicates entry point for serving model.
  *
  * Datasets are row-oriented, so you must declare input/output as list of rows.
  *
  * @tparam Input input row type
  * @tparam Output output row type
  */
trait ServingJob[Input <: DataSet, Output <: DataSet] {
  def execute(data: List[Input]): List[Output]
}

object ServingJob {
  type generalizedType = ServingJob[_ <: io.hydrosphere.serving.runtime.scala.lib.DataSet, _ <: io.hydrosphere.serving.runtime.scala.lib.DataSet]
}

