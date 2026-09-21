package de.wittig.database.jcr

import munit.FunSuite

import java.nio.file.Files

import javax.jcr.{Repository, Session}

class OakDemoTest extends FunSuite:

  private def withRepo[A](f: (Repository, Session) => A): A =
    val repoDir                 = Files.createTempDirectory("oak-test").toFile
    val (repository, fileStore) = OakDemo.open(repoDir)
    val session                 = OakDemo.login(repository)
    try f(repository, session)
    finally
      session.logout()
      fileStore.close()
      os.remove.all(os.Path(repoDir))

  test("Hierarchischer Content-Baum mit typisierten Properties") {
    withRepo { (_, session) =>
      OakDemo.seedBlog(session, OakDemo.blogPosts)

      val root = session.getRootNode
      assert(root.hasNode("content"), "Wurzelknoten 'content' fehlt")
      assert(root.hasNode("content/posts"), "posts-Container fehlt")
      assert(root.hasNode("content/posts/perf-tuning"), "Post-Knoten fehlt")

      val post = session.getNode("/content/posts/perf-tuning")
      assertEquals(post.getProperty("title").getString, "JVM Performance Tuning")
      assertEquals(post.getProperty("category").getString, "scala")
      assertEquals(post.getProperty("likes").getLong, 42L)
      assertEquals(post.getProperty("tags").getValues.map(_.getString).toList, List("jvm", "scala"))
    }
  }

  test("JCR-SQL2 Query findet Posts per Kategorie") {
    withRepo { (_, session) =>
      OakDemo.seedBlog(session, OakDemo.blogPosts)

      assertEquals(OakDemo.findTitlesByCategory(session, "scala").toSet, Set("JVM Performance Tuning", "Akka Cluster Patterns"))
      assertEquals(OakDemo.findTitlesByCategory(session, "kotlin"), List("Kafka with Ktor"))
      assertEquals(OakDemo.findTitlesByCategory(session, "unbekannt"), Nil)
    }
  }

  test("Versionierung: checkin -> aendern -> als alte Version wiederherstellen") {
    withRepo { (_, session) =>
      OakDemo.seedBlog(session, OakDemo.blogPosts)

      val result   = OakDemo.demonstrateVersioning(session, "/content/posts/perf-tuning")
      val expected = OakDemo.blogPosts.head

      assertEquals(result.restoredTitle, expected.title)
      assert(result.versionCount >= 2L, s"Version-History sollte wachsen, war aber ${result.versionCount}")
    }
  }

  test("Session-Isolation: aenderungen erst nach save() sichtbar") {
    withRepo { (repository, session) =>
      OakDemo.seedBlog(session, OakDemo.blogPosts)
      val reader = OakDemo.login(repository)

      try
        val draft = session.getNode("/content/posts").addNode("draft")
        draft.setProperty("title", "Noch nicht veroeffentlicht")
        // Vor save() ist die Aenderung fuer andere Sessions unsichtbar
        assert(!reader.getRootNode.hasNode("content/posts/draft"), "vor save() verletzt")
        session.save()
        reader.refresh(false)
        assert(reader.getRootNode.hasNode("content/posts/draft"), "nach save() verletzt")
      finally reader.logout()
    }
  }
