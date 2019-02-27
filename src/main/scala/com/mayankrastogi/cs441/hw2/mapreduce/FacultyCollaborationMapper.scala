package com.mayankrastogi.cs441.hw2.mapreduce

import com.mayankrastogi.cs441.hw2.utils.{Settings, Utils}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import javax.xml.parsers.SAXParserFactory
import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapreduce.Mapper

import scala.xml.{Elem, XML}

class FacultyCollaborationMapper extends Mapper[LongWritable, Text, Text, IntWritable] with LazyLogging {

  private val settings = new Settings(ConfigFactory.load())

  private val facultyLookupTable: Map[String, String] = Utils.getFacultyLookupTable(settings.facultyListFile)
  private val dblpDTDPath = Utils.getDTDFilePath(settings.dblpDTDAbsolutePath, settings.dblpDTDResourcePath)
  private val edgeIndicator = settings.collaboratorsEdgeIndicator

  private val xmlParser = SAXParserFactory.newInstance().newSAXParser()

  private val one = new IntWritable(1)
  private val authorKey = new Text()


  override def map(key: LongWritable, value: Text, context: Mapper[LongWritable, Text, Text, IntWritable]#Context): Unit = {
    logger.trace(s"Mapper invoked: map(key: $key, value: ${value.toString}")

    val publicationElement = createValidXML(value.toString)
    val authors = extractSortedListOfUICFacultyFromPublication(publicationElement)

    if (authors.nonEmpty) {
      generateFacultyPairs(authors).foreach { pair =>
        authorKey.set(pair)
        logger.info(s"Mapper emit: <$authorKey, ${one.get()}>")
        context.write(authorKey, one)
      }
    }
  }

  def createValidXML(publicationXMLString: String): Elem = {

    val xmlString =
      s"""<?xml version="1.0" encoding="ISO-8859-1"?><!DOCTYPE dblp SYSTEM "$dblpDTDPath"><dblp>$publicationXMLString</dblp>"""

    XML.withSAXParser(xmlParser).loadString(xmlString)
  }

  def extractSortedListOfUICFacultyFromPublication(publicationElement: Elem): Seq[String] = {

    val authorLookupTag = publicationElement.child.head.label match {
      case "book" | "proceedings" => "editor"
      case _ => "author"
    }

    (publicationElement \\ authorLookupTag).collect({ case node if facultyLookupTable contains node.text => facultyLookupTable(node.text) }).sorted
  }

  def generateFacultyPairs(faculty: Seq[String]): Iterator[String] = {
    faculty.size match {
      case 0 => Iterator()
      case 1 => faculty.toIterator
      case _ => faculty.toIterator ++ faculty.combinations(2).map(_.sorted.mkString(edgeIndicator))
    }
  }
}
