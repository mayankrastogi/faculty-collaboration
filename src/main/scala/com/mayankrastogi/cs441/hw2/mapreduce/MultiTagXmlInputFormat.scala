package com.mayankrastogi.cs441.hw2.mapreduce

import java.io.IOException
import java.nio.charset.StandardCharsets

import com.google.common.io.Closeables
import com.mayankrastogi.cs441.hw2.mapreduce.MultiTagXmlInputFormat.MultiTagXmlRecordReader
import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.{DataOutputBuffer, LongWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.{FileSplit, TextInputFormat}
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}

class MultiTagXmlInputFormat extends TextInputFormat with LazyLogging {

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[LongWritable, Text] = {
    try {
      new MultiTagXmlRecordReader(split.asInstanceOf[FileSplit], context.getConfiguration)
    }
    catch {
      case ioe: IOException =>
        logger.warn("Error while creating XmlRecordReader", ioe)
        null
    }
  }
}

object MultiTagXmlInputFormat {

  val START_TAG_KEY = "multitagxmlinput.start"
  val END_TAG_KEY = "multitagxmlinput.end"

  @throws[IOException]
  class MultiTagXmlRecordReader(split: FileSplit, conf: Configuration)
    extends RecordReader[LongWritable, Text] {

    private val startTags = conf.getStrings(START_TAG_KEY).map(_.getBytes(StandardCharsets.UTF_8))
    private val endTags = conf.getStrings(END_TAG_KEY).map(_.getBytes(StandardCharsets.UTF_8))

    private val startTagToEndTagMapping = startTags.zip(endTags).toMap

    // open the file and seek to the start of the split
    private val start = split.getStart
    private val end = start + split.getLength

    private val fsin = split.getPath.getFileSystem(conf).open(split.getPath)
    fsin.seek(start)

    private val buffer = new DataOutputBuffer()
    private val currentKey = new LongWritable()
    private val currentValue = new Text()

    private var matchedTag = Array[Byte]()

    override def nextKeyValue(): Boolean = {
      readNext(currentKey, currentValue)
    }

    @throws[IOException]
    private def readNext(key: LongWritable, value: Text): Boolean = {
      if (fsin.getPos < end && readUntilMatch(startTags, false)) {
        try {
          buffer.write(matchedTag)
          if (readUntilMatch(Array(startTagToEndTagMapping(matchedTag)), true)) {
            key.set(fsin.getPos)
            value.set(buffer.getData, 0, buffer.getLength)
            return true
          }
        }
        finally {
          buffer.reset()
        }
      }
      false
    }

    private def readUntilMatch(tags: Array[Array[Byte]], lookingForEndTag: Boolean): Boolean = {
      val matchCounter: Array[Int] = tags.indices.map(_ => 0).toArray

      while (true) {
        val currentByte = fsin.read()
        // end of file
        if (currentByte == -1) {
          return false
        }
        // save to buffer
        if (lookingForEndTag) {
          buffer.write(currentByte)
        }

        // Check if we are matching any of the tags
        tags.indices.foreach { tagIndex =>
          val tag = tags(tagIndex)
          if (currentByte == tag(matchCounter(tagIndex))) {
            matchCounter(tagIndex) += 1
            if (matchCounter(tagIndex) >= tag.length) {
              matchedTag = tag
              return true
            }
          }
          else {
            matchCounter(tagIndex) = 0
          }
        }
        // Check if we've passed the stop point
        if (!lookingForEndTag && matchCounter.forall(_ == 0) && fsin.getPos >= end) {
          return false
        }
      }
      false
    }

    override def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {}

    override def getCurrentKey: LongWritable = {
      new LongWritable(currentKey.get())
    }

    override def getCurrentValue: Text = {
      new Text(currentValue)
    }

    override def getProgress: Float = (fsin.getPos - start) / (end - start).toFloat

    override def close(): Unit = Closeables.close(fsin, true)
  }

}