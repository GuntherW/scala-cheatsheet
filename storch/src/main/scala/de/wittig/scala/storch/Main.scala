package de.wittig.scala.storch
import torch.Device.{CPU, CUDA}
import torch.*

// https://storch.dev/tutorial/buildmodel.html
object Main extends App:

  val device = if torch.cuda.isAvailable then CUDA else CPU

  class NeuralNetwork extends nn.Module:
    val flatten         = nn.Flatten()
    val linearReluStack = register(nn.Sequential(
      nn.Linear(28 * 28, 512),
      nn.ReLU(),
      nn.Linear(512, 512),
      nn.ReLU(),
      nn.Linear(512, 10),
    ))

    def apply(x: Tensor[Float32]) =
      val flattened = flatten(x)
      val logits    = linearReluStack(flattened)
      logits

  val model = NeuralNetwork().to(device)
  println(model)
  println(device)
