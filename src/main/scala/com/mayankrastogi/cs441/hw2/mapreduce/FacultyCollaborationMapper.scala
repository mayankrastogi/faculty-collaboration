package com.mayankrastogi.cs441.hw2.mapreduce

import com.mayankrastogi.cs441.hw2.utils.{Settings, Utils}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import javax.xml.parsers.SAXParserFactory
import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapreduce.Mapper

import scala.xml.{Elem, XML}

/**
  * Finds authors and emits singles/pairs for the found authors.
  *
  * The key value pairs are of the form <"Faculty1 -- Faculty2", 1>, denoting that Faculty1 collaborated with Faculty2
  * for a publication. The faculty names are sorted before using as keys to ensure that "Faculty2 -- Faculty1" key is
  * never generated by any mapper for the same pair of faculty, since both of them denote the same set of faculty.
  */
class FacultyCollaborationMapper extends Mapper[LongWritable, Text, Text, IntWritable] with LazyLogging {

  // Load application settings
  private val settings = new Settings(ConfigFactory.load())

  private val facultyLookupTable: Map[String, String] = Utils.getFacultyLookupTable(settings.facultyListFile)
  private val dblpDTDPath = Utils.getDTDFilePath(settings.dblpDTDAbsolutePath, settings.dblpDTDResourcePath)
  private val edgeIndicator = settings.collaboratorsEdgeIndicator

  // Cache XML parser instance as instantiating a parser is expensive. The same parser can be reused to parse multiple
  // inputs. Moreover, the DTD file will not be needed to parsed for each new input and the grammar pool will keep
  // getting updated, thus improving the performance of the job. This optimization reduces the time taken by the job to
  // run by 40%.
  private val xmlParser = SAXParserFactory.newInstance().newSAXParser()

  private val one = new IntWritable(1)
  private val authorKey = new Text()


  override def map(key: LongWritable, value: Text, context: Mapper[LongWritable, Text, Text, IntWritable]#Context): Unit = {
    logger.trace(s"Mapper invoked: map(key: $key, value: ${value.toString}")

    // Load the XML
    val publicationElement = createValidXML(value.toString)

    // Extract the list of authors that belong to UIC CS department and sort them in lexicographical order
    val authors = extractSortedListOfUICFacultyFromPublication(publicationElement)

    if (authors.nonEmpty) {
      // Emit one entry for each UIC author, and a combination (nCr) of each author
      generateFacultyPairs(authors).foreach { pair =>
        authorKey.set(pair)
        logger.info(s"Mapper emit: <$authorKey, ${one.get()}>")
        context.write(authorKey, one)
      }
    }
  }

  /**
    * Wraps the input XML string between the dblp.xml's DOCTYPE so that HTML entities can be understood by the XML
    * parser.
    *
    * @param publicationXMLString The XML string received from the XmlInputFormat.
    * @return The parsed XML node with `dblp` as the root node.
    */
  def createValidXML(publicationXMLString: String): Elem = {

    // Wrap the XML subset in a format which will be valid according to the DTD
    val xmlString =
      s"""<?xml version="1.0" encoding="ISO-8859-1"?><!DOCTYPE dblp SYSTEM "$dblpDTDPath"><dblp>$publicationXMLString</dblp>"""

    // Parse the XML using the cached XML parser
    XML.withSAXParser(xmlParser).loadString(xmlString)
  }

  /**
    * Extracts any UIC CS department faculty names from the XML document, maps them to their primary names (as they
    * appear on the UIC website), and sorts them in lexicographical order.
    *
    * @param publicationElement The root node of the XML document (The root must be &lt;dblp&gt;)
    * @return Sorted list of UIC faculty found in the document (if any)
    */
  def extractSortedListOfUICFacultyFromPublication(publicationElement: Elem): Seq[String] = {

    // Determine the tag under which the faculty names may be present depending on the type of publication
    // For <book> and <proceedings>, faculty names are found under <editor> tag. For all other type of publications,
    // faculty names are found under <author>
    val authorLookupTag = publicationElement.child.head.label match {
      case "book" | "proceedings" => "editor"
      case _ => "author"
    }

    // Find the authors, map them to their primary names using the lookup table, and then sort them
    (publicationElement \\ authorLookupTag).collect({ case node if facultyLookupTable contains node.text => facultyLookupTable(node.text) }).sorted
  }

  /**
    * Generates combinations (nCr) of faculty members found, and joins them using a separator as specified in
    * application settings.
    *
    * @param faculty List of faculty members found in the XML document, sorted in lexicographical order.
    * @return Individual faculty names, along with all possible combinations (not permutations) of the faculty members
    */
  def generateFacultyPairs(faculty: Seq[String]): Iterator[String] = {
    faculty.size match {
        // If the list of faculty is empty, return an empty list
      case 0 => Iterator()
        // If there is only one faculty, return only the faculty
      case 1 => faculty.toIterator
        // If there are more than one faculty, return all the individual faculty names, along with all possible
        // combinations of the faculty, with each pair joined by the `edgeIndicator`
      case _ => faculty.toIterator ++ faculty.combinations(2).map(_.sorted.mkString(edgeIndicator))
    }
  }
}
