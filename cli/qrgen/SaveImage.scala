//> using dep eu.joaocosta::qrgen::0.1.0
//> using dep eu.joaocosta::minart::0.6.4

import eu.joaocosta.minart.backend.defaults.given
import eu.joaocosta.minart.graphics.*
import eu.joaocosta.minart.graphics.image.Image
import eu.joaocosta.minart.runtime.Resource
import eu.joaocosta.qrgen.*

@main
def saveImage =

  val qrCode = QrCode.encodeText("https://www.scala-lang.org/", Ecc.LOW)

  val normalSurface = RamSurface(
    qrCode.map(_.map(b => Color.grayscale(if (b) 0 else 255)))
  )

  val buntSurface = RamSurface(
    qrCode.map(_.map(b => Color.fromARGB(if (b) 0xff0000ff else 0xffffff00)))
  )

  Image.storeBmpImage(normalSurface.view.scale(4), Resource("qrcode4.bmp"))
  Image.storeBmpImage(buntSurface.view.scale(16), Resource("qrcode16.bmp"))
