package de.wittig.database.jcr

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.jcr.Jcr
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders
import org.apache.jackrabbit.oak.segment.file.FileStore
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder

import java.io.File
import java.nio.file.Files

import javax.jcr.*
import javax.jcr.query.Query

final case class BlogPost(slug: String, title: String, category: String, likes: Long, tags: List[String])
final case class VersioningResult(restoredTitle: String, versionCount: Long)

object OakDemo:

  val blogPosts: List[BlogPost] = List(
    BlogPost("perf-tuning", "JVM Performance Tuning", "scala", 42L, List("jvm", "scala")),
    BlogPost("akka-cluster", "Akka Cluster Patterns", "scala", 77L, List("akka", "distributed")),
    BlogPost("ktor-kafka", "Kafka with Ktor", "kotlin", 13L, List("kotlin", "kafka")),
    BlogPost("playwright-e2e", "Playwright End to End Tests", "java", 21L, List("testing", "java"))
  )

  def open(storeDir: File): (Repository, FileStore) =
    val fileStore  = FileStoreBuilder.fileStoreBuilder(storeDir).build()
    val nodeStore  = SegmentNodeStoreBuilders.builder(fileStore).build()
    val repository = new Jcr(new Oak(nodeStore)).createRepository()
    (repository, fileStore)

  def login(repository: Repository): Session =
    repository.login(new SimpleCredentials("admin", "admin".toCharArray))

  def seedBlog(session: Session, posts: List[BlogPost]): Unit =
    val root     = session.getRootNode
    val content  = root.addNode("content")
    val allPosts = content.addNode("posts")
    posts.foreach { p =>
      val post = allPosts.addNode(p.slug)
      post.setProperty("title", p.title)
      post.setProperty("category", p.category)
      post.setProperty("likes", p.likes)
      post.setProperty("tags", p.tags.toArray)
    }
    session.save()

  def findTitlesByCategory(session: Session, category: String): List[String] =
    val sql2  =
      "SELECT * FROM [nt:unstructured] AS p WHERE ISDESCENDANTNODE(p, '/content/posts') AND p.[category] = $category"
    val query = session.getWorkspace.getQueryManager.createQuery(sql2, Query.JCR_SQL2)
    query.bindValue("category", session.getValueFactory.createValue(category))
    val nodes = query.execute.getNodes

    val builder = List.newBuilder[String]
    while nodes.hasNext do builder += nodes.nextNode.getProperty("title").getString
    builder.result()

  def demonstrateVersioning(session: Session, postPath: String): VersioningResult =
    val post = session.getNode(postPath)
    post.addMixin("mix:versionable")
    session.save()

    val versionManager = session.getWorkspace.getVersionManager
    val firstVersion   = versionManager.checkin(postPath)
    val originalTitle  = session.getNode(postPath).getProperty("title").getString

    versionManager.checkout(postPath)
    session.getNode(postPath).setProperty("title", originalTitle + " (bearbeitet)")
    session.save()
    versionManager.checkin(postPath)

    val versionIter  = versionManager.getVersionHistory(postPath).getAllVersions
    val versionCount = Iterator.continually(versionIter).takeWhile(_.hasNext).map(_ => versionIter.nextVersion()).length
    versionManager.restore(firstVersion, true)
    session.save()
    VersioningResult(session.getNode(postPath).getProperty("title").getString, versionCount)

  def main(args: Array[String]): Unit =
    println("=== JackRabbit Oak embedded (SegmentStore / Tar) ===")

    val storeDir                = Files.createTempDirectory("oak-demo").toFile
    val (repository, fileStore) = OakDemo.open(storeDir)
    val writer                  = OakDemo.login(repository)
    val reader                  = OakDemo.login(repository)

    println(s"Repository: $storeDir")

    try
      println()
      println("--- 1. Hierarchisches Schema-freies Content-Modell ---")
      OakDemo.seedBlog(writer, OakDemo.blogPosts)
      println("Content-Baum /content/posts/<slug> mit typisierten Properties angelegt (title, category, likes, tags)")

      val firstPost = writer.getNode("/content/posts/perf-tuning")
      println(s"readback likes=${firstPost.getProperty("likes").getLong} tags=${firstPost.getProperty("tags").getValues.map(_.getString).mkString(",")}")

      println()
      println("--- 2. Transaktionale Sessions ---")
      val draft             = writer.getNode("/content/posts").addNode("draft")
      draft.setProperty("title", "Noch nicht veroeffentlicht")
      val visibleBeforeSave = reader.getRootNode.hasNode("content/posts/draft")
      println(s"vor save() fuer andere Session sichtbar: $visibleBeforeSave")
      writer.save()
      reader.refresh(false)
      val visibleAfterSave  = reader.getRootNode.hasNode("content/posts/draft")
      println(s"nach save() fuer andere Session sichtbar: $visibleAfterSave")

      println()
      println("--- 3. Abfragen (JCR-SQL2) ---")
      val scalaPosts = OakDemo.findTitlesByCategory(writer, "scala")
      println(s"Posts der Kategorie 'scala': ${scalaPosts.mkString(", ")}")

      println()
      println("--- 4. Eingebaute Versionierung ---")
      val result = OakDemo.demonstrateVersioning(writer, "/content/posts/perf-tuning")
      println(s"Versionen in der History: ${result.versionCount}")
      println(s"Titel nach restore auf die erste Version: '${result.restoredTitle}'")
    finally
      writer.logout()
      reader.logout()
      fileStore.close()
