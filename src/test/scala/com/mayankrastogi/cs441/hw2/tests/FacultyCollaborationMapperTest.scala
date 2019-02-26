package com.mayankrastogi.cs441.hw2.tests

import com.mayankrastogi.cs441.hw2.mapreduce.FacultyCollaborationMapper
import com.mayankrastogi.cs441.hw2.utils.Settings
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}

class FacultyCollaborationMapperTest extends FunSuite with Matchers {

  private val mapper = new FacultyCollaborationMapper
  private val keySeparator = new Settings(ConfigFactory.load()).collaboratorsEdgeIndicator

  private val xmlWithNoUICFaculty =
    <article mdate="2017-05-20" key="journals/ac/ChellappanD13">
      <author>Sriram Chellappan</author>
      <author>Neelanjana Dutta</author>
      <title>Mobility in Wireless Sensor Networks.</title>
      <pages>185-222</pages>
      <year>2013</year>
      <volume>90</volume>
      <journal>Advances in Computers</journal>
      <ee>https://doi.org/10.1016/B978-0-12-408091-1.00003-8</ee>
      <url>db/journals/ac/ac90.html#ChellappanD13</url>
    </article>

  private val xmlWithOneUICFaculty =
    <article mdate="2017-05-20" key="journals/ac/BikasAG16">
      <author>Md. Abu Naser Bikas</author>
      <author>Abdullah Alourani</author>
      <author>Mark Grechanik</author>
      <title>How Elasticity Property Plays an Important Role in the Cloud: A Survey.</title>
      <pages>1-30</pages>
      <year>2016</year>
      <volume>103</volume>
      <journal>Advances in Computers</journal>
      <ee>https://doi.org/10.1016/bs.adcom.2016.04.001</ee>
      <url>db/journals/ac/ac103.html#BikasAG16</url>
    </article>

  private val xmlWithTwoUICFacultyAndNotSorted =
    <article mdate="2017-05-27" key="journals/fmsd/SloanB97">
      <author>Ugo A. Buy</author>
      <author>Robert H. Sloan</author>
      <title>Stubborn Sets for Real-Time Petri Nets.</title>
      <pages>23-40</pages>
      <year>1997</year>
      <volume>11</volume>
      <journal>Formal Methods in System Design</journal>
      <number>1</number>
      <url>db/journals/fmsd/fmsd11.html#SloanB97</url>
      <ee>https://doi.org/10.1023/A:1008629725384</ee>
    </article>

  private val xmlWithFacultyWithAlternateNameListedWithPrimaryName =
    <inproceedings mdate="2017-05-24" key="conf/icst/GrechanikHB13">
      <author>Mark Grechanik</author>
      <author>B. M. Mainul Hossain</author>
      <author>Ugo Buy</author>
      <title>Testing Database-Centric Applications for Causes of Database Deadlocks.</title>
      <pages>174-183</pages>
      <year>2013</year>
      <booktitle>ICST</booktitle>
      <ee>https://doi.org/10.1109/ICST.2013.19</ee>
      <ee>http://doi.ieeecomputersociety.org/10.1109/ICST.2013.19</ee>
      <crossref>conf/icst/2013</crossref>
      <url>db/conf/icst/icst2013.html#GrechanikHB13</url>
    </inproceedings>

  test("Empty sequence should be returned if no UIC faculty are part of an article") {
    val authors = mapper.extractSortedListOfUICFacultyFromPublication(xmlWithNoUICFaculty)

    authors shouldBe empty
  }

  test("Only UIC faculty should be extracted from a valid article") {
    val authors = mapper.extractSortedListOfUICFacultyFromPublication(xmlWithOneUICFaculty)

    authors should not be empty
    authors should contain only "Mark Grechanik"
  }

  test("UIC faculty with alternate names should get mapped with primary name when publication lists primary name") {
    val authors = mapper.extractSortedListOfUICFacultyFromPublication(xmlWithFacultyWithAlternateNameListedWithPrimaryName)

    authors should not contain "Ugo A. Buy"
    authors should contain ("Ugo Buy")
  }

  test("UIC faculty with alternate names should get mapped with primary name when publication lists alternate name") {
    val authors = mapper.extractSortedListOfUICFacultyFromPublication(xmlWithTwoUICFacultyAndNotSorted)

    authors should not contain "Ugo A. Buy"
    authors should contain ("Ugo Buy")
  }

  test("UIC faculty with no alternate names should get mapped with primary name") {
    val authors = mapper.extractSortedListOfUICFacultyFromPublication(xmlWithOneUICFaculty)

    authors should contain ("Mark Grechanik")
  }

  test("UIC faculty extracted from an article should be sorted") {
    val authors = mapper.extractSortedListOfUICFacultyFromPublication(xmlWithTwoUICFacultyAndNotSorted)

    authors should have size 2
    authors should contain allOf("Robert Sloan", "Ugo Buy")
    authors shouldBe sorted
  }

  test("Empty author list should generate empty faculty pairs") {
    val pairs = mapper.generateFacultyPairs(Seq())

    pairs shouldBe empty
  }

  test("Author list of size 1 should generate one item") {
    val pairs = mapper.generateFacultyPairs(Seq("Mark Grechanik")).toList

    pairs should not be empty
    pairs should contain only "Mark Grechanik"
  }

  test(s"Author list of size 2 should contain entries for the 2 authors individually along with one author pair, joined with $keySeparator") {
    val pairs = mapper.generateFacultyPairs(Seq("Mark Grechanik", "Ugo A. Buy")).toList

    pairs should have size 3
    pairs should contain allOf(
        "Mark Grechanik",
        "Ugo A. Buy",
        s"Mark Grechanik${keySeparator}Ugo A. Buy"
      )
  }

  test(s"Author list of size 3 should contain entries for the 3 authors individually along with 3 author pairs, each joined with $keySeparator") {
    val pairs = mapper.generateFacultyPairs(Seq("Mark Grechanik", "Ugo A. Buy", "Robert H. Sloan")).toList

    pairs should have size 6
    pairs should contain allOf(
      "Mark Grechanik",
      "Robert H. Sloan",
      "Ugo A. Buy",
      s"Mark Grechanik${keySeparator}Ugo A. Buy",
      s"Robert H. Sloan${keySeparator}Ugo A. Buy",
      s"Mark Grechanik${keySeparator}Robert H. Sloan"
    )
  }
}
