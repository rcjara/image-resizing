import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;

public class SeamCarver {
  private Picture pict;
  private int width;
  private int height;
  private boolean direction;
  private double[][] accumulatedEnergy;
  private int[][] from;

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

  private Bounds bounds() {
    if (!direction) {
      return new Bounds(width, height);
    } else {
      return new Bounds(height, width);
    }
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
    if (direction != flip || accumulatedEnergy == null) {
      generateAccumulatedEnergy(flip);
    }

    direction = flip;
    int bound1 = bounds().getB1();
    int bound2 = bounds().getB2();

    //choose min root for path
    double min = Double.POSITIVE_INFINITY;
    int minJ = -1;
    for (int j = 0; j < bound2; j++) {
      if (accumulatedEnergy[bound1 - 1][j] < min) {
        minJ = j;
        min = accumulatedEnergy[bound1 - 1][j];
      }
    }

    //create min path
    int[] path = new int[bound1];
    int curJ = minJ;
    for (int i = bound1 - 1; i >= 0; i--) {
      path[i] = curJ;
      curJ += from[i][curJ];
    }

    return path;
  }

  private void generateAccumulatedEnergy(boolean flip) {
    direction = flip;
    int bound1 = bounds().getB1();
    int bound2 = bounds().getB2();

    //create accumulation matrix
    accumulatedEnergy = new double[bound1][bound2];
    for (int i = 0; i < bound2; i++) { accumulatedEnergy[0][i] = 0; }

    from = new int[bound1][bound2];

    for (int i = 1; i < bound1; i++) {
      for (int j = 0; j < bound2; j++) {
        accumulatedEnergy[i][j] = newAccumulatedEnergy(i, j);
      }
    }
  }

  private double newAccumulatedEnergy(int i, int j) {
    double min1 = Double.POSITIVE_INFINITY;
    double min2 = Double.POSITIVE_INFINITY;
    double min3 = Double.POSITIVE_INFINITY;
    double curEnergy = getEnergy(i, j, direction);

    int bound2 = !direction ? height : width;

    if (j > 0) min1 = curEnergy + accumulatedEnergy[i - 1][j - 1];
    min2 = curEnergy + accumulatedEnergy[i - 1][j];
    if (j < bound2 - 1) min3 = curEnergy + accumulatedEnergy[i - 1][j + 1];

    if (min1 <= min2 && min1 <= min3) {
      from[i][j] = -1;
      return min1;
    } else if (min2 <= min1 && min2 <= min3) {
      from[i][j] = 0;
      return min2;
    } else {
      from[i][j] = 1;
      return min3;
    }
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
    direction = false;

    for (int i = 0; i < width; i++) {
      for (int j = a[i]; j < height; j++) {
        pict.set(i, j, pict.get(i, j + 1));;
      }
    }

    removeSeam(a);
  }

  public void removeVerticalSeam(int[] a) {  // remove vertical seam from picture
    if (a.length != height) { throw new IllegalArgumentException(); }
    width--;
    direction = true;

    for (int j = 0; j < height; j++) {
      for (int i = a[j]; i < width; i++) {
        pict.set(i, j, pict.get(i + 1, j));
      }
    }

    removeSeam(a);
  }

  private void removeSeam(int[] a) {
    int bound1 = bounds().getB1();
    int bound2 = bounds().getB2();

    int[] minChanged = new int[a.length];
    int[] maxChanged = new int[a.length];

    for (int i = 0; i < a.length; i++) {
      minChanged[i] = a[i];
      maxChanged[i] = a[i];
    }

    //remove the actual cells from the array
    for (int i = 1; i < bound1; i++) {
      for (int j = a[i]; j < bound2; j++) {
        accumulatedEnergy[i][j] = accumulatedEnergy[i][j + 1];
        from[i][j] = from[i][j + 1];
      }
    }

    for (int i = 1; i < bound1; i++) {
      int jStart = Math.max(0, minChanged[i - 1] - 2);
      int jStop = Math.min(bound2, maxChanged[i - 1] + 2);

      for (int j = jStart; j < jStop; j++) {
        double newEnergy = newAccumulatedEnergy(i, j);
        if (newEnergy != accumulatedEnergy[i][j]) {
          accumulatedEnergy[i][j] = newEnergy;
          if (j < minChanged[i]) { minChanged[i] = j; }
          if (j > maxChanged[i]) { maxChanged[i] = j; }
        }
      }
    }
  }

  public static void main(String[] args) {
    Picture pict = new Picture("HJoceanSmall.png");
    SeamCarver sc = new SeamCarver(pict);
    sc.picture().show();
  }

  private class Bounds {
    private int bounds1;
    private int bounds2;

    public Bounds(int b1, int b2) {
      bounds1 = b1;
      bounds2 = b2;
    }

    public int getB1() { return bounds1; }
    public int getB2() { return bounds2; }
  }
}

