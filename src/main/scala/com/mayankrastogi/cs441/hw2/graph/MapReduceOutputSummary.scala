package com.mayankrastogi.cs441.hw2.graph

/**
  * Holds a summary of the map-reduce output, such as all the faculty (nodes), and collaborations (edges) between them.
  *
  * @param faculties All UIC CS faculty found in the dblp.xml file
  * @param collaborations All collaborations between pairs of UIC CS faculty.
  * @param maxPublicationsByFaculty Maximum number of publications by any faculty
  * @param maxCollaborationsByFaculty Maximum number of shared publications between two UIC CS faculty
  */
class MapReduceOutputSummary(val faculties: List[Faculty],
                             val collaborations: List[Collaboration],
                             val maxPublicationsByFaculty: Int,
                             val maxCollaborationsByFaculty: Int) {

  // Auxiliary constructor for convenience of initialization
  def this() = this(List(), List(), 0, 0)
}
