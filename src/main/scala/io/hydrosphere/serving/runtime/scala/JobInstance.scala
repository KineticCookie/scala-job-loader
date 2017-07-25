package io.hydrosphere.serving.runtime.scala

import java.io.File
import java.util.jar.JarFile

import io.hydrosphere.serving.runtime.scala.lib.{DataSet, ServingJob}

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

/**
  * Job instance for Scala
  * Support only objects(singletons)
  *
  * @param clazz  - original class
  */
class JobInstance(val clazz: Class[_], val inputType: Type, val outputType: Type) {
  private[this] val mirror = universe.runtimeMirror(clazz.getClassLoader)
  private val ctor = mirror
    .reflectClass(inputType.typeSymbol.asClass)
    .reflectConstructor(inputType.typeSymbol.asClass.primaryConstructor.asMethod)

  def run(params: List[Map[String, Any]]): List[DataSet] = {
    val batch = params.map { row =>
      val args = validateParams(row)
      createInputDataSet(args.right.get)
    }
    val instance = createInstance()
    instance.execute(batch)
  }

  private def createInstance(): ServingJob[DataSet, DataSet] = {
    val staticModule = mirror.staticModule(clazz.getName)
    val obj = mirror.reflectModule(staticModule)
    obj.instance.asInstanceOf[ServingJob[DataSet, DataSet]]
  }

  def createInputDataSet(args: Seq[AnyRef]): DataSet = {
    ctor.apply(args: _*).asInstanceOf[DataSet]
  }

  def validateParams(params: Map[String, Any]): Either[Throwable, Seq[AnyRef]] = {
    val validated: Seq[Either[Throwable, Any]] = arguments.map({case (name, tpe) =>
      val param = params.get(name)
      validateParam(tpe, name, param)
    })

    if (validated.exists(_.isLeft)) {
      val errors = validated.collect({ case Left(e) => e.getMessage }).mkString("(", ",", ")")
      val msg = s"Param validation errors: $errors"
      Left(new IllegalArgumentException(msg))
    } else {
      val p = validated.collect({case Right(x) => x.asInstanceOf[AnyRef]})
      Right(p)
    }
  }

  private def validateParam(tpe: Type, name: String, value: Option[Any]): Either[Throwable, Any] = {
    value match {
      // ignore optional arguments if they not presented
      case x if tpe.erasure =:= typeOf[Option[Any]] => Right(value)
      case Some(x) => Right(x)
      case None =>
        val msg = s"Missing argument name: $name, type: $tpe"
        Left(new IllegalArgumentException(msg))
    }
  }

  private def arguments: Seq[(String, Type)] =
    ctor.symbol.paramLists.head.map(s => s.name.toString -> s.typeSignature)

  def argumentsTypes: Map[String, Type] =
    arguments.toMap
}

object JobInstance {
  import io.hydrosphere.serving.runtime.scala.lib.reflection.PumpedJar

  def fromJar(file: File): JobInstance = {
    val jarFile = new PumpedJar(file)
    val clazz = jarFile.getSubclasses[ServingJob.generalizedType].head
    val types = jarFile.getSuperclassTypeArgs(clazz, classOf[ServingJob.generalizedType])
    new JobInstance(clazz, types.head, types.last)
  }
}