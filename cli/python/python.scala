//> using python

import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters

import scala.util.Random
import scala.math.{Pi, sin, random}

object PlotDemo extends App {

  val sequences = generate3DataSeqs

  py.local {
    val plt = py.module("matplotlib.pyplot")

    for {
      (seq, color) <- sequences.zip(Seq("b", "r", "g"))
    } {
      plt.plot(seq.toPythonProxy, color = color)
      plt.show()
    }
  }

  def generate3DataSeqs: Seq[Seq[Double]] = {
    val amplitude = 1.0 // Amplitude of the sine wave
    val numSamples = 1000
    val numSequences = 3
    val noiseAmplitude = 0.2 // Amplitude of noise

    // Generate three sequences with varying numbers of cycles
    val sequences = (1 to numSequences).map { seqIdx =>
      val frequency = seqIdx // Varying frequency for each sequence
      (1 to numSamples).map { sampleIdx =>
        val noise = (random * 2 - 1) * noiseAmplitude // Generate random noise
        val phase = 2 * Pi * frequency * sampleIdx / numSamples
        amplitude * sin(phase) + noise
      }
    }
    sequences
  }
}
