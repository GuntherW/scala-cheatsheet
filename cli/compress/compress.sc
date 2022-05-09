//> using lib "org.apache.commons:commons-compress:1.21"
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.compressors.*

private val s = "Mein schöner String, Mein schöner String, Mein schöner String, Mein schöner String, Kein schöner String"

def compress(input: Array[Byte], algo:String): Array[Byte] =
  val bos        = new ByteArrayOutputStream()
  val gzip       = new CompressorStreamFactory().createCompressorOutputStream(algo, bos)
  gzip.write(input)
  gzip.close()
  val compressed = bos.toByteArray
  bos.close()
  compressed

println(new String(compress(s.getBytes("UTF-8"), CompressorStreamFactory.GZIP)).length)
println(new String(compress(s.getBytes("UTF-8"), CompressorStreamFactory.BZIP2)).length)
println(new String(compress(s.getBytes("UTF-8"), CompressorStreamFactory.LZ4_BLOCK)).length)
println(new String(compress(s.getBytes("UTF-8"), CompressorStreamFactory.LZ4_FRAMED)).length)
