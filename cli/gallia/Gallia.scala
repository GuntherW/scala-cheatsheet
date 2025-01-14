//> using dep "io.github.galliaproject::gallia-core::0.6.1"

import gallia.*

@main def main(): Unit =
  """{"foo": "hallo", "bar": 1, "baz": true, "qux": "world"}"""
    .read() // will infer schema if none is provided
    .toUpperCase("foo")
    .increment("bar")
    .remove("qux")
    .nest("baz")
    .under("parent")
    .rename("parent" |> "baz")
    .to("Baz")
    .printJson()
