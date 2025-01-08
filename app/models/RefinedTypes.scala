package models

import eu.timepit.refined.api._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

object RefinedTypes {
  type PageCount = Int Refined Interval.Closed[0, 2500]
  type NonEmptyString = String Refined NonEmpty
  type AlphaNumeric = String Refined MatchesRegex["^[a-zA-Z0-9]*$"]
}
