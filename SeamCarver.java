import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;

public class SeamCarver {
  private Picture pict;
  private int width;
  private int height;

  public SeamCarver(Picture picture) {
    pict = picture;
    width = pict.width();
    height = pict.height();
    picture();
  }

  public Picture picture() {                      // current picture
    Picture newPict = new Picture(width, height);
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        newPict.set(i, j, pict.get(i, j));
      }
    }

    pict = newPict;
    return pict;
  }

  public int width() {                        // width of current picture
    return width;
  }

  public int height() {                       // height of current picture
    return height;
  }

  public double energy(int x, int y) {           // energy of pixel at column x and row y
    if (x < 0 || x > width - 1 ||
        y < 0 || y > height - 1) {
      throw new IndexOutOfBoundsException();
    }
    if (x == 0 || x == width - 1 ||
        y == 0 || y == height - 1) {
      return 195075.0;
    }

    double diffX = colorDiff(x - 1, y, x + 1, y);
    double diffY = colorDiff(x, y - 1, x, y + 1);

    return diffX + diffY;
  }

  private int[] minPath(boolean flip) {
    int bound1;
    int bound2;

    if (!flip) {
      bound1 = width;
      bound2 = height;
    } else {
      bound1 = height;
      bound2 = width;
    }

    //create accumulation matrix
    double[] accum = new double[bound2];
    for (int i = 0; i < bound2; i++) { accum[i] = 0; }
    double[] prev = accum;

    int[][] from = new int[bound1][bound2];

    for (int i = 1; i < bound1; i++) {
      accum = new double[bound2];

      for (int j = 0; j < bound2; j++) {
        double min1 = Double.POSITIVE_INFINITY;
        double min2 = Double.POSITIVE_INFINITY;
        double min3 = Double.POSITIVE_INFINITY;
        double curEnergy = getEnergy(i, j, flip);

        if (j > 0) min1 = curEnergy + prev[j - 1];
        min2 = curEnergy + prev[j];
        if (j < bound2 - 1) min3 = curEnergy + prev[j + 1];

        if (min1 <= min2 && min1 <= min3) {
          accum[j] = min1;
          from[i][j] = j - 1;
        } else if (min2 <= min1 && min2 <= min3) {
          accum[j] = min2;
          from[i][j] = j;
        } else {
          accum[j] = min3;
          from[i][j] = j + 1;
        }
      }

      prev = accum;
    }

    //choose min root for path
    double min = Double.POSITIVE_INFINITY;
    int minJ = -1;
    for (int j = 0; j < bound2; j++) {
      if (accum[j] < min) {
        minJ = j;
        min = accum[j];
      }
    }

    //create min path
    int[] path = new int[bound1];
    int curJ = minJ;
    for (int i = bound1 - 1; i >= 0; i--) {
      path[i] = curJ;
      curJ = from[i][curJ];
    }

    return path;
  }

  private double getEnergy(int i, int j, boolean flip) {
    if (!flip) {
      return energy(i, j);
    } else {
      return energy(j, i);
    }
  }

  private double colorDiff(int x1, int y1, int x2, int y2) {
    Color a = pict.get(x1, y1);
    Color b = pict.get(x2, y2);

    double acc = 0.0;

    acc += componentDiff(a.getRed(), b.getRed());
    acc += componentDiff(a.getGreen(), b.getGreen());
    acc += componentDiff(a.getBlue(), b.getBlue());

    return acc;
  }

  private double componentDiff(int a, int b) {
    return Math.pow(b - a, 2);
  }

  public int[] findHorizontalSeam() {           // sequence of indices for horizontal seam
    return minPath(false);
  }

  public int[] findVerticalSeam() {
    return minPath(true);
  }

  public void removeHorizontalSeam(int[] a) {  // remove horizontal seam from picture
    if (a.length != width) { throw new IllegalArgumentException(); }
    height--;

    for (int i = 0; i < width; i++) {
      for (int j = a[i]; j < height; j++) {
        pict.set(i, j, pict.get(i, j + 1));;
      }
    }
  }

  public void removeVerticalSeam(int[] a) {  // remove vertical seam from picture
    if (a.length != height) { throw new IllegalArgumentException(); }
    width--;

    for (int j = 0; j < height; j++) {
      for (int i = a[j]; i < width; i++) {
        pict.set(i, j, pict.get(i + 1, j));
      }
    }
  }

  public static void main(String[] args) {
    Picture pict = new Picture("HJoceanSmall.png");
    SeamCarver sc = new SeamCarver(pict);
    sc.picture().show();
  }
}

