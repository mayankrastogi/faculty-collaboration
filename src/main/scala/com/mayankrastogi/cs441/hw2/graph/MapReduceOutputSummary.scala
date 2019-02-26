package com.mayankrastogi.cs441.hw2.graph

class MapReduceOutputSummary(val faculties: List[Faculty],
                             val collaborations: List[Collaboration],
                             val maxPublicationsByFaculty: Int,
                             val maxCollaborationsByFaculty: Int) {

  def this() = this(List(), List(), 0, 0)
}
