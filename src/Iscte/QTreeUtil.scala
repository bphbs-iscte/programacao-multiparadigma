package Iscte

import Iscte.GUI.FxApp
import Iscte.QTree.Coords
import java.awt.Color

case class QTreeUtil(qt: QTree[Coords]) {
  def scale(scale: Double): QTree[Coords] = QTreeUtil.scale(scale, this.qt)

  def mirrorV(): QTree[Coords] = QTreeUtil.mirrorV(this.qt)

  def mirrorH(): QTree[Coords] = QTreeUtil.mirrorH(this.qt)

  def rotateL(): QTree[Coords] = QTreeUtil.rotateL(this.qt)

  def rotateR(): QTree[Coords] = QTreeUtil.rotateR(this.qt)

  def mapColorEffect(f: Color => Color): QTree[Coords] = QTreeUtil.mapColorEffect(f, this.qt)

  def notPureNoise(c: Color): Color = QTreeUtil.notPureNoise(c)

  def contrast(c: Color): Color = QTreeUtil.contrast(c)

  def sepia(c: Color): Color = QTreeUtil.sepia(c)
}

object QTreeUtil {
  /** Magnification/reduction operation on an image, according to the factor provided. */
  def scale(sc: Double, qt: QTree[Coords]): QTree[Coords] = {
    qt match {
      case QNode(cd: Coords, one, two, three, four) =>
        QNode((((cd._1._1 * sc).toInt, (cd._1._2 * sc).toInt),
          ((cd._2._1 * sc).toInt, (cd._2._2 * sc).toInt)), scale(sc, one), scale(sc, two), scale(sc, three), scale(sc, four))
      case QLeaf((cd: Coords, color)) =>
        QLeaf((((cd._1._1 * sc).toInt, (cd._1._2 * sc).toInt),
          ((cd._2._1 * sc).toInt, (cd._2._2 * sc).toInt)), color)
      case _ => QEmpty
    }
  }

  /** Given a QTree it calculates the max coordinate value. */
  private def maxTreeCord(qt: QTree[Coords], value: Int): Int = {
    qt match {
      case QLeaf((cd: Coords, _)) =>
        if (value == 1) cd._2._1
        else cd._2._2
      case QNode(cd, _, _, _, _) =>
        if (value == 1) cd._2._1
        else cd._2._2
      case _ => 0
    }
  }

  /** Vertical mirroring operation. */
  def mirrorV(qt: QTree[Coords]): QTree[Coords] = {
    val max = maxTreeCord(qt, 1)

    def aux(qt: QTree[Coords], max: Int): QTree[Coords] = {
      qt match {
        case QNode(cd, one, two, three, four) =>
          QNode(((max - cd._2._1, cd._1._2), (max - cd._1._1, cd._2._2)), aux(two, max), aux(one, max), aux(four, max), aux(three, max))
        case QLeaf((cd: Coords, color)) =>
          QLeaf(((max - cd._2._1, cd._1._2), (max - cd._1._1, cd._2._2)), color)
        case _ => QEmpty
      }
    }

    aux(qt, max)
  }

  /** Horizontal mirroring operation. */
  def mirrorH(qt: QTree[Coords]): QTree[Coords] = {
    val max = maxTreeCord(qt, 0)

    def aux(qt: QTree[Coords], max: Int): QTree[Coords] = {
      qt match {
        case QNode(cd, one, two, three, four) =>
          QNode(((cd._1._1, max - cd._2._2), (cd._2._1, max - cd._1._2)), aux(three, max), aux(four, max), aux(one, max), aux(two, max))
        case QLeaf((cd: Coords, color)) =>
          QLeaf(new Coords((cd._1._1, max - cd._2._2), (cd._2._1, max - cd._1._2)), color)
        case _ => QEmpty
      }
    }

    aux(qt, max)
  }

  /** 90 degree rotation to the left. */
  def rotateL(qt: QTree[Coords]): QTree[Coords] = {
    val max_x = maxTreeCord(qt, 1)

    def aux(qt: QTree[Coords], max_x: Int): QTree[Coords] = {
      qt match {
        case QNode(cd, one, two, three, four) =>
          QNode(((cd._1._2, max_x - cd._2._1), (cd._2._2, max_x - cd._1._1)), aux(two, max_x), aux(four, max_x), aux(one, max_x), aux(three, max_x))
        case QLeaf((cd: Coords, color)) =>
          QLeaf(new Coords((cd._1._2, max_x - cd._2._1), (cd._2._2, max_x - cd._1._1)), color)
        case _ => QEmpty
      }
    }

    aux(qt, max_x)
  }

  /** 90 degree rotation to the right. */
  def rotateR(qt: QTree[Coords]): QTree[Coords] = {
    val max_y = maxTreeCord(qt, 0)

    def aux(qt: QTree[Coords], max_y: Int): QTree[Coords] = {
      qt match {
        case QNode(cd, one, two, three, four) =>
          QNode(((max_y - cd._2._2, cd._1._1), (max_y - cd._1._2, cd._2._1)), aux(three, max_y), aux(one, max_y), aux(four, max_y), aux(two, max_y))
        case QLeaf((cd: Coords, color)) =>
          QLeaf(new Coords((max_y - cd._2._2, cd._1._1), (max_y - cd._1._2, cd._2._1)), color)
        case _ => QEmpty
      }
    }

    aux(qt, max_y)
  }

  /** Checks whether the given value is valid as an RGB component. */
  private def colorComponentInRange(component: Int): Int = {
    if (component > 255) 255
    else if (component < 0) 0
    else component
  }

  /** Change the color components given according to a provided factor. */
  private def changeColor(c: Color, value: Double): Color = {
    val r = colorComponentInRange((c.getRed * value).toInt)
    val g = colorComponentInRange((c.getGreen * value).toInt)
    val b = colorComponentInRange((c.getBlue * value).toInt)
    new Color(r, g, b)
  }

  /** Uniform mapping of a function onto the entire image. */
  def mapColorEffect(f: Color => Color, qt: QTree[Coords]): QTree[Coords] = {
    qt match {
      case QNode(value, one, two, three, four) =>
        QNode(value, mapColorEffect(f, one), mapColorEffect(f, two), mapColorEffect(f, three), mapColorEffect(f, four))
      case QLeaf((value, color: Color)) => QLeaf((value, f(color)))
      case _ => QEmpty
    }
  }

  /** Uniform mapping of a function onto the entire image.
   * It only works with pureNoise. */
  def mapColorEffect_1(qt: QTree[Coords], random: Random): (QTree[Coords], Random) = {
    qt match {
      case QNode(value, one, two, three, four) =>
        (QNode(value, mapColorEffect_1(one, FxApp.r)._1, mapColorEffect_1(two, FxApp.r)._1, mapColorEffect_1(three, FxApp.r)._1, mapColorEffect_1(four, FxApp.r)._1), FxApp.r)
      case QLeaf((value, color: Color)) => {
        (QLeaf((value, pureNoise(color, FxApp.r)._1)), pureNoise(color, FxApp.r)._2)
      }
      case _ => (QEmpty, random)
    }
  }

  /** Obtains the pure noise value of a RGB color. */
  def pureNoise(c: Color, r: Random): (Color, Random) = {
    val i = r.nextInt(2)
    i._1 match {
      case 0 => FxApp.r = i._2; (c, i._2)
      case 1 => FxApp.r = i._2; (Color.black, i._2)
    }
  }

  /** Obtains the not pure noise value of a RGB color. */
  def notPureNoise(c: Color): Color = {
    val random = new scala.util.Random
    val noise = random.nextFloat
    if (noise < 0.5) Color.black
    else c
  }

  /** Obtains the contrast of a RGB color. */
  def contrast(c: Color): Color = {
    val red = c.getRed
    val green = c.getGreen
    val blue = c.getBlue
    val luminance = ImageUtil.luminance(red, green, blue)
    if (luminance < 128) changeColor(c, 0.8)
    else changeColor(c, 1.2)
  }

  /** Obtains the sepia of a RGB color. */
  def sepia(c: Color): Color = {
    val avg = (c.getRed + c.getGreen + c.getBlue) / 3
    val depth = 20
    val intensity = 30
    val red = colorComponentInRange(avg + (2 * depth))
    val green = colorComponentInRange(avg + depth)
    val blue = colorComponentInRange(c.getBlue - intensity)
    new Color(red, green, blue)
  }
}