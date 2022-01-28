val string = "b日d"

val bytesArray = string.getBytes("UTF-8")
val unicode    = string.codePoints().toArray.toList

bytesArray.map(b => Integer.toBinaryString(b | 0x100).takeRight(8)).toList
bytesArray.map(_.toChar)
bytesArray.map(_.toChar.toInt)
bytesArray.map(_.toChar).mkString

new String(bytesArray, "UTF-8")

26085.toChar
Integer.parseInt("110010111100101", 2)

// UTF-8 Codierung:
// 1-Byte-Sequenz: 0.... => 7-Bit
// 2-Byte-Sequenz: 110.. 10....  => 5 Bit + 6 Bit = 11 Bit
// 3-Byte-Sequenz: 1110. 10.... 10....  => 4 Bit + 6 Bit + 6 Bit = 16 Bit
