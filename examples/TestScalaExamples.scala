// Test Scala examples for code quality analysis

object ExampleScala {
  
  // Example with pattern matching complexity
  def complexPatternMatch(x: Any): String = x match {
    case s: String if s.length > 10 => "Long string"
    case s: String if s.length > 5 => "Medium string"
    case s: String => "Short string"
    case i: Int if i > 100 => "Large number"
    case i: Int if i > 50 => "Medium number"
    case i: Int => "Small number"
    case _ => "Unknown"
  }
  
  // Example with implicit usage
  implicit val timeout: Int = 5000
  implicit val retries: Int = 3
  
  def withTimeout[T](f: => T)(implicit timeout: Int): T = f
  
  // Example with nested for-comprehension
  def nestedForComprehension(lists: List[List[Int]]): List[Int] = {
    for {
      outer <- lists
      inner <- outer if inner > 5
      doubled = inner * 2
    } yield doubled
  }
  
  // Example with large case class
  case class LargeCaseClass(
    field1: String,
    field2: Int,
    field3: Boolean,
    field4: Double,
    field5: Long,
    field6: Float,
    field7: Char,
    field8: String,
    field9: Int,
    field10: Boolean,
    field11: Double,
    field12: Long
  )
  
  // Example with unsafe Option.get
  def unsafeOptionGet(opt: Option[String]): String = {
    opt.get // This should trigger warning
  }
  
  // Example with safe Option handling
  def safeOptionHandling(opt: Option[String]): String = {
    opt.getOrElse("default")
  }
  
  // Example with TODO marker
  def incompleteFunction(): Unit = {
    // TODO: implement this later
    println("Not implemented")
  }
  
  // Example with long method
  def veryLongMethod(): Unit = {
    println("Line 1")
    println("Line 2")
    println("Line 3")
    println("Line 4")
    println("Line 5")
    println("Line 6")
    println("Line 7")
    println("Line 8")
    println("Line 9")
    println("Line 10")
    println("Line 11")
    println("Line 12")
    println("Line 13")
    println("Line 14")
    println("Line 15")
    println("Line 16")
    println("Line 17")
    println("Line 18")
    println("Line 19")
    println("Line 20")
    println("Line 21")
    println("Line 22")
    println("Line 23")
    println("Line 24")
    println("Line 25")
    println("Line 26")
    println("Line 27")
    println("Line 28")
    println("Line 29")
    println("Line 30")
    println("Line 31")
    println("Line 32")
    println("Line 33")
    println("Line 34")
    println("Line 35")
    println("Line 36")
    println("Line 37")
    println("Line 38")
    println("Line 39")
    println("Line 40")
    println("Line 41")
    println("Line 42")
    println("Line 43")
    println("Line 44")
    println("Line 45")
    println("Line 46")
    println("Line 47")
    println("Line 48")
    println("Line 49")
    println("Line 50")
    println("Line 51")
    println("Line 52")
  }
}
