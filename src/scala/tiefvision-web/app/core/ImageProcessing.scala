/**
  * Copyright (C) 2016 Pau Carré Cardona - All Rights Reserved
  * You may use, distribute and modify this code under the
  * terms of the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt).
  */

package core

import java.io.File
import java.nio.file.{Files, Paths}

import play.api.Logger
import controllers.Configuration
import db.BoundingBox
import scala.sys.process.Process

object ImageProcessing {

  lazy val Images = new File(Configuration.DbImagesFolder).listFiles().map(_.getName)
  lazy val SimilarityImages = new File(Configuration.SimilarityImagesFolder).listFiles().map(_.getName)
  val random = scala.util.Random

  def randomImage = Images(random.nextInt(Images.length))

  def randomSimilarityImage = SimilarityImages(random.nextInt(SimilarityImages.length))

  lazy val luaConfigFlag: String = {
    Option(sys.props("luaConfig")) match {
      case Some(path) => s"-config $path"
      case _ => ""
    }
  }

  def findSimilarImages(image: String, finderFolder: String, imagesFolderOpt: Option[String] = None): ImageSearchResult = {
    val luaSearch = s"luajit search.lua $image ${imagesFolderOpt.getOrElse("")} $luaConfigFlag"
    val finderProcessBuilder = Process(Seq("bash", "-c", luaSearch), new File(finderFolder))
    val finderProcessOutput: String = finderProcessBuilder.!!
    val data = {
      def reduce(lines: Seq[(String, Double)], line: String): Seq[(String, Double)] = {
        similarityLineToSimilarityResult(line) match {
          case Some((file: String, similarity: Double)) => lines :+(file: String, similarity: Double)
          case None => lines
        }
      }
      finderProcessOutput.lines.foldLeft(Seq[(String, Double)]())(reduce)
    }
    val similaritySimilarityMap = data.map(e => {
      val (file: String, similarity: Double) = e
      similarity -> file
    }).sortBy(-_._1)
    ImageSearchResult(image, similaritySimilarityMap)
  }

  object SimilarityFinder {
    def getSimilarityFinder(isSupervised: Boolean) = {
      if(isSupervised) s"${Configuration.HomeFolder}/src/torch/15-deeprank-searcher-db"
      else s"${Configuration.HomeFolder}/src/torch/10-similarity-searcher-cnn-db"
    }
  }

  val findSimilarImagesFromFileFolder = s"${Configuration.HomeFolder}/src/torch/11-similarity-searcher-cnn-file"

  def similarityLineToSimilarityResult(similarityLine: String) = {
    val pattern = "(.+)\\s(.+)".r
    similarityLine match {
      case pattern(filePath, similarity) => Some(filePath.split("\\/").last, similarity.toDouble)
      case _ => None
    }
  }


  def saveImageScaled(scaledBoundingBox: BoundingBox, scale: Int) = {
    import sys.process._
    val destinationFile = s"${Configuration.ScaledImagesFolder}/${scaledBoundingBox.name}_$scale"
    if (!Files.exists(Paths.get(destinationFile))) {
      val sourceFile = s"${Configuration.DbImagesFolder}/${scaledBoundingBox.name}"
      val scaleImagesCommand = s"convert $sourceFile -resize ${scaledBoundingBox.width}x $destinationFile"
      scaleImagesCommand !!
    }
  }

  def boundingBoxTypeFolder(extendBoundingBox: Boolean) = if (extendBoundingBox) "extended" else "original"

  def generateBackgroundCrops(scaledBoundingBox: BoundingBox, scale: Int, extendBoundingBox: Boolean) = {
    import sys.process._
    if (scaledBoundingBox.width >= Configuration.CropSize && scaledBoundingBox.height >= Configuration.CropSize) {
      var samples = 0
      var attempts = 0
      while (samples < Configuration.NumSamples && attempts < Configuration.NumSamples * 5) {
        attempts = attempts + 1
        val randomCrop = generateRandomCrop(scaledBoundingBox)
        if (!randomCrop.intersectsWith(scaledBoundingBox.toCrop)) {
          val diff = scaledBoundingBox.toCrop minus randomCrop
          val destinationFilename =
            s"${scaledBoundingBox.name}___${scaledBoundingBox.width}_${scaledBoundingBox.height}_" +
              s"${diff.left}_${diff.top}_${diff.right + Configuration.CropSize}_${diff.bottom + Configuration.CropSize}.jpg"
          val destinationFilePath = s"${Configuration.BackgroundCropImagesFolder}/${boundingBoxTypeFolder(extendBoundingBox)}/$destinationFilename"
          val sourceFilePath = s"${Configuration.ScaledImagesFolder}/${scaledBoundingBox.name}_$scale"
          samples = samples + 1
          s"convert $sourceFilePath -crop ${Configuration.CropSize}x${Configuration.CropSize}+${randomCrop.left}+${randomCrop.top} -type truecolor $destinationFilePath" !!;
        }
      }
    }
  }

  def generateCrops(scaledBoundingBox: BoundingBox, scale: Int, extendBoundingBox: Boolean) = {
    import sys.process._
    if (scaledBoundingBox.width >= Configuration.CropSize && scaledBoundingBox.height >= Configuration.CropSize) {
      var samples = 0
      var attempts = 0
      while (samples < Configuration.NumSamples && attempts < Configuration.NumSamples * 5) {
        attempts = attempts + 1
        val extendedBoundingBox = {
          if (extendBoundingBox)
            scaledBoundingBox.copy(
              top = math.max(1, scaledBoundingBox.top - (scaledBoundingBox.bottom - scaledBoundingBox.top) * 0.10).toInt,
              bottom = math.min(scaledBoundingBox.height, scaledBoundingBox.bottom + (scaledBoundingBox.bottom - scaledBoundingBox.top) * 0.50).toInt)
          else scaledBoundingBox
        }
        val randomCrop = generateRandomCrop(scaledBoundingBox)
        if (randomCrop.intersectsMoreThan50PercentWith(extendedBoundingBox.toCrop)) {
          val diff = scaledBoundingBox.toCrop minus randomCrop
          val destinationFilename =
            s"${scaledBoundingBox.name}___${scaledBoundingBox.width}_${scaledBoundingBox.height}_" +
              s"${diff.left}_${diff.top}_${diff.right + Configuration.CropSize}_${diff.bottom + Configuration.CropSize}.jpg"
          val destinationFilePath = s"${Configuration.CropImagesFolder}/${boundingBoxTypeFolder(extendBoundingBox)}/$destinationFilename"
          val sourceFilePath = s"${Configuration.ScaledImagesFolder}/${scaledBoundingBox.name}_$scale"
          samples = samples + 1
          s"convert $sourceFilePath -crop ${Configuration.CropSize}x${Configuration.CropSize}+${randomCrop.left}+${randomCrop.top} -type truecolor $destinationFilePath" !!;
        }
      }
    }
  }

  def generateRandomCrop(boundingBox: BoundingBox) = {
    val left = random.nextInt(boundingBox.width - Configuration.CropSize + 1)
    val right = left + Configuration.CropSize
    val top = random.nextInt(boundingBox.height - Configuration.CropSize + 1)
    val bottom = top + Configuration.CropSize
    Crop(left = left, right = right, top = top, bottom = bottom)
  }

}
