//> using scala 3.9.0

// Dezimiert Laufzeit-Warnungen auf neueren JDKs (hier JDK 27) - rein kosmetisch, ändert nichts am Programmverhalten:
//   - `sloth` patcht das von Scala 3 erzeugte Bytecode-Muster für `lazy val`, das sonst `sun.misc.Unsafe::objectFieldOffset`
//     nutzt (siehe `LazyVals$`-Warnung) und ab JDK 24 eine "terminally deprecated"-Warnung auslöst (JEP 498).
//   - `--enable-native-access=ALL-UNNAMED` erlaubt den nativen JNI-Zugriff, den `onnxruntime` (für das lokale
//     Embedding-Modell, siehe `EmbeddingModels.scala`) beim Laden seiner nativen Bibliothek braucht, ohne Warnung (JEP 472).
//   - `slf4j-nop` ist ein No-Op-Logging-Backend: langchain4j nutzt intern SLF4J, ohne eine konkrete Implementierung auf dem
//     Klassenpfad meldet SLF4J das bei jedem Start ("No SLF4J providers were found") - dieses Projekt braucht aber keine
//     Logs, nur die Konsolenausgabe von `Main.scala`.
//> using sloth
//> using javaOpt --enable-native-access=ALL-UNNAMED
//> using dep org.slf4j:slf4j-nop:2.0.20

//> using dep dev.langchain4j:langchain4j:1.21.0
//> using dep dev.langchain4j:langchain4j-anthropic:1.21.0
//> using dep dev.langchain4j:langchain4j-document-parser-apache-pdfbox:1.21.0-beta31
//> using dep dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:1.21.0-beta31
//> using dep dev.langchain4j:langchain4j-pgvector:1.21.0-beta31
//> using dep org.apache.pdfbox:pdfbox:3.0.8
//> using dep org.postgresql:postgresql:42.7.13
//> using dep com.lihaoyi::os-lib::0.11.8
//> using test.dep org.scalameta::munit::1.3.6
