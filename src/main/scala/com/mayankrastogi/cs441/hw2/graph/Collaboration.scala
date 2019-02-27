package com.mayankrastogi.cs441.hw2.graph

/**
  * Holds information regarding an edge in the graph.
  *
  * @param faculty1 The name of the first faculty
  * @param faculty2 The name of the second faculty
  * @param publications Number of publications shared by the two faculty
  */
class Collaboration(val faculty1: String, val faculty2: String, val publications: Int)