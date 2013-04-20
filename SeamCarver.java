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
      return 195075.0;
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
    double found = 0;
    for (int j = 0; j < bound2; j++) {
      if (accumulatedEnergy[bound1 - 1][j] < min) {
        minJ = j;
        min = accumulatedEnergy[bound1 - 1][j];
        found = 1;
      } else if (accumulatedEnergy[bound1 - 1][j] == min) {
        found++;
        if (Math.random() < 1 / found) {
          minJ = j;
        }
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

    //remove the actual cells from the array
    for (int i = 1; i < bound1; i++) {
      for (int j = a[i]; j < bound2; j++) {
        accumulatedEnergy[i][j] = accumulatedEnergy[i][j + 1];
        from[i][j] = from[i][j + 1];
      }
    }

    calculateChanges(a);
  }

  private void calculateChanges(int[] a) {
    int bound1 = bounds().getB1();
    int bound2 = bounds().getB2();

    int[] minChanged = new int[a.length];
    int[] maxChanged = new int[a.length];

    for (int i = 0; i < a.length; i++) {
      minChanged[i] = a[i];
      maxChanged[i] = a[i];
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

  private void growPictHeight() {
    if (pict.height() > height) { return; }

    Picture newPict = new Picture(pict.width(), pict.height() * 2);

    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        newPict.set(i, j, pict.get(i, j));
      }
    }

    pict = newPict;
  }

  private void growInternalArrays() {
    int bound1 = bounds().getB1();
    int bound2 = bounds().getB2();

    if (from[0].length > bound2) { return; }

    for (int i = 0; i < bound1; i++) {
      int[] newFrom = new int[bound2 * 2];
      double[] newAccumulatedEnergy = new double[bound2 * 2];
      for (int j = 0; j < bound2; j++) {
        newFrom[j] = from[i][j];
        newAccumulatedEnergy[j] = accumulatedEnergy[i][j];
      }
      from[i] = newFrom;
      accumulatedEnergy[i] = newAccumulatedEnergy;
    }
  }

  public void addHorizontalSeam(int[] a) {
    growPictHeight();
    growInternalArrays();
    height++;
    for (int i = 0; i < width; i++) {
      for (int j = height; j > a[i]; j--) {
        accumulatedEnergy[i][j] = accumulatedEnergy[i][j - 1];
        from[i][j] = from[i][j - 1];
        pict.set(i, j, pict.get(i, j - 1));
      }
    }

    int[] b = new int[width];
    for (int i = 0; i < width; i++) { b[i] = a[i]; }

    //Offset[] offsets = new Offset[width];
    Color[] newPixels = new Color[width];
    for (int i = 0; i < width; i++) {
      newPixels[i] = pict.get(i, b[i]);
    }


    //*/
    newPixels[0] = pict.get(0, b[0]);
    newPixels[width - 1] = pict.get(width - 1, b[width - 1]);
    for (int i = 1; i < width - 1; i++) {
      double thisEnergy = getEnergy(i, b[i], false);
      double prevEnergy = getEnergy(i, b[i - 1], false);
      double nextEnergy = getEnergy(i, b[i + 1], false);

      if (prevEnergy < nextEnergy) {
        newPixels[i] = pict.get(i + 1, b[i + 1]);
      } else {
        newPixels[i] = pict.get(i - 1, b[i - 1]);
      }

      if (prevEnergy < thisEnergy && nextEnergy < thisEnergy) {
        newPixels[i] = blend(pict.get(i - 1, b[i - 1]), pict.get(i + 1, b[i + 1]));
      }
    }
   //*/

    for (int i = 0; i < width; i++) {
      pict.set(i, b[i], newPixels[i]);
    }

/*
    for (int i = 0; i < width; i++) {
      Offset offset = offsets[i];
      int x = i + offset.getI();
      int y = b[i] + offset.getJ();
      x = x < 0 ? 0 : x;
      x = width > x ? x : width - 1;
      y = y < 0 ? 0 : y;
      y = height > y ? y : height - 1;
      pict.set(i, b[i], pict.get(x, y));

      //replaceWithLeastNeighbor(i, b[i]);
    }
//*/

    calculateChanges(a);
  }

  private void shuffle(Color[] a) {
    int N = a.length;
    for (int i = 0; i < N; i++) {
      // int from remainder of deck
      int r = i + (int) Math.floor(Math.random() * (N - i));
      Color swap = a[r];
      a[r] = a[i];
      a[i] = swap;
    }
  }

  private Color blend(Color c1, Color c2) {
     return new Color((c1.getRed() + c2.getRed()) / 2,
                      (c1.getGreen() + c2.getGreen()) / 2,
                      (c1.getBlue() + c2.getBlue()) / 2);
  }

  private Offset replaceWithLeastNeighbor(int i, int j) {
    int iOffset = 0;
    int jOffset = 0;
    double leastFound = 0;

    for (int io = -1; io < 2; io++) {
      for (int jo = -1; jo < 2; jo++) {
        if (io == 0 && jo == 0) { continue; }
        if (i + io < 0 || j + jo < 0 || i + io >= width || j + jo >= height) { continue; }

        double energy = getEnergy(i + io, j + jo, false);
        if (energy > leastFound) {
          leastFound = energy;
          iOffset = io;
          jOffset = jo;
        }
      }
    }

    return new Offset(iOffset, jOffset);
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

  private class Offset {
    private int i;
    private int j;

    public Offset(int _i, int _j) {
      i = _i;
      j = _j;
    }

    public int getI() { return i; }
    public int getJ() { return j; }
  }
}

