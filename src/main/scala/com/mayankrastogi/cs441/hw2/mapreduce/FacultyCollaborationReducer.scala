package com.mayankrastogi.cs441.hw2.mapreduce

import java.lang

import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapreduce.Reducer

import scala.collection.JavaConverters._

class FacultyCollaborationReducer extends Reducer[Text, IntWritable, Text, IntWritable] with LazyLogging {

  override def reduce(key: Text, values: lang.Iterable[IntWritable], context: Reducer[Text, IntWritable, Text, IntWritable]#Context): Unit = {
    logger.trace(s"Reducer invoked: reduce(key: ${key.toString}, values: ${values.asScala.map(_.get())}")

    val sum = values.asScala.fold(new IntWritable(0))((a, b) => new IntWritable(a.get() + b.get()))
    logger.info(s"Reducer emit: <$key, ${sum.get()}>")
    context.write(key, sum)
  }
}
