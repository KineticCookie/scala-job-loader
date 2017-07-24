package io.hydrosphere.serving.runtime.scala

import java.io.File
import java.net.URL
import java.util.jar._
import java.util.jar.JarEntry

import io.hydrosphere.serving.runtime.scala.lib.ServingJob

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.reflect.runtime.universe
import universe._

import scala.collection.JavaConversions._

/**
  * Created by bulat on 20/07/2017.
  */

object Main extends App {
  def getCaseFields(caseType: Type): List[String] = {
    caseType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor =>
        m.name.toString
    }.toList
  }

  val file = new File("example/target/scala-2.11/example.jar")
  val jar = new JarFile(file)
  val loader = new URLClassLoader(Array(file.toURI.toURL), getClass.getClassLoader)
  val mirror = universe.runtimeMirror(getClass.getClassLoader)

  val clazz = jar.entries()
    .filter (!_.isDirectory)
    .map(_.getName)
    .filter(n => n.endsWith(".class") && !(n.startsWith("scala/") || n.startsWith("java/")))
    .map(_.split(".class")(0).replace('/', '.'))
    .map(loader.loadClass)
    .filterNot(mirror.classSymbol(_).toType.erasure =:= typeOf[ServingJob.generalizedType])
    .filter(mirror.classSymbol(_).toType <:< typeOf[ServingJob.generalizedType])
    .toList.head

  val cltype = mirror.classSymbol(clazz).toType.typeSymbol.asClass
  println(s"Class ${cltype.name} detected:")
  val basetype = typeOf[ServingJob.generalizedType].typeSymbol.asClass
  val bb = internal.thisType(cltype).baseType(basetype)

  val inputType = bb.typeArgs.head
  val outputType = bb.typeArgs.last

  val inputs = getCaseFields(inputType)
  val outputs = getCaseFields(outputType)
  println(s"Inputs: $inputs")
  println(s"Outputs: $outputs")
}
