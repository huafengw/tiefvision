package controllers

import java.io.File

object Configuration {

  val HomeFolder = sys.env("TIEFVISION_HOME")
  val CropSize = 224
  val NumSamples = 5
  val BoundingBoxesFolder = s"$HomeFolder/resources/bounding-boxes"
  val ScaledImagesFolder = s"$HomeFolder/resources/bounding-boxes/scaled-images"
  val CropImagesFolder = s"$HomeFolder/resources/bounding-boxes/crops"
  val BackgroundCropImagesFolder = s"$HomeFolder/resources/bounding-boxes/background-crops"
  val DbImagesFolder = s"$HomeFolder/resources/dresses-db/master"
  val SimilarityImagesFolder = s"$HomeFolder/src/torch/data/db/similarity/img-enc-cnn"
  val UploadedImagesFolder = s"$HomeFolder/resources/dresses-db/uploaded/master"
  val scaleLevels = Seq(2, 3)
  val testPercentage = 0.05

  def mkdirIfNotExist(dir: String): Unit = {
    val dir = new File(dir)
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }

  def mkdirs(): Unit = {
    mkdirIfNotExist(BoundingBoxesFolder)
    mkdirIfNotExist(ScaledImagesFolder)
    mkdirIfNotExist(CropImagesFolder)
    mkdirIfNotExist(BackgroundCropImagesFolder)
    mkdirIfNotExist(DbImagesFolder)
    mkdirIfNotExist(SimilarityImagesFolder)
    mkdirIfNotExist(UploadedImagesFolder)
    mkdirIfNotExist(s"$CropImagesFolder/extended")
    mkdirIfNotExist(s"$CropImagesFolder/original")
    mkdirIfNotExist(s"$BackgroundCropImagesFolder/extended")
    mkdirIfNotExist(s"$BackgroundCropImagesFolder/original")
  }

  mkdirs()
}
