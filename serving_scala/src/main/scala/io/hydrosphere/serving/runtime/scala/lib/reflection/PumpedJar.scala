package io.hydrosphere.serving.runtime.scala.lib.reflection

import java.io.File

import scala.collection.JavaConversions._
import java.util.jar.JarFile

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

import scala.reflect.runtime.universe
import universe._

class PumpedJar(file: File) {
  private val jarFile = new JarFile(file)
  private val loader = new URLClassLoader(Seq(file.toURI.toURL), getClass.getClassLoader)
  private val mirror = universe.runtimeMirror(loader)

  /**
    * Reads jar and retrieves `.class` entries
    * @return
    */
  def getClasses: Iterator[String] = {
    jarFile.entries()
      .filter(!_.isDirectory)
      .map(_.getName)
      .filter(n => n.endsWith(".class"))
      .map(_.split(".class")(0).replace('/', '.'))
  }

  /**
    * Loads all except scala and java std classes
    * @return
    */

  def loadClasses: Seq[Class[_]] = {
    getClasses
      .filterNot(x => x.startsWith("scala.") || x.startsWith("java.") )
      .map(loader.loadClass)
      .toSeq
  }

  /**
    * Returns subclasses of given class, except itself.
    * @tparam T Superclass
    * @return List of subclasses
    */
  def getSubclasses[T](implicit tag: TypeTag[T]): Seq[Class[_]] = {
    this.loadClasses
      .filterNot(mirror.classSymbol(_).toType.erasure =:= typeOf[T])
      .filter(mirror.classSymbol(_).toType <:< typeOf[T])
  }

  /**
    * Returns type list of superclass.
    *
    * For example for `class Foo extends Bar[String, Int]` method returns List(typeOf[String], typeOf[Int]).
    *
    * @param subClass
    * @param superClass
    * @return
    */
  def getSuperclassTypeArgs(subClass: Class[_], superClass: Class[_]): List[universe.Type] = {
    val subType = mirror.classSymbol(subClass).toType.typeSymbol.asClass
    val superType = mirror.classSymbol(superClass).toType.typeSymbol.asClass
    val bb = internal.thisType(subType).baseType(superType)
    bb.typeArgs
  }

  /**
    * Returns method mirror of a constructor for a given type.
    * @param typee
    * @return
    */
  def getTypeCtor(typee: universe.Type): universe.MethodMirror = {
    val inCompanion = typee.typeSymbol.asClass
    mirror.reflectClass(inCompanion).reflectConstructor(inCompanion.primaryConstructor.asMethod)
  }

  /**
    * Gets `object` instance with given class
    * @param clazz
    * @tparam T
    * @return
    */
  def getObjectAs[T](clazz: Class[_]): T = {
    val staticModule = mirror.staticModule(clazz.getName)
    val obj = mirror.reflectModule(staticModule)
    obj.instance.asInstanceOf[T]
  }

}

