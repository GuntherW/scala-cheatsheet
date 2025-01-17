object TestMax:

  // If using Int (not BigInt) instead, stainless finds counter examples. -> Overflow
  def max(x: BigInt, y: BigInt): BigInt = {
    if (x - y > 0) x
    else y
  }.ensuring(res =>
    x <= res && y <= res && (res == x || res == y)
  )
