import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.util.*;
import java.awt.*;
/*
* wrapper class for computer vision image processing functions to use.
* Use: to build a computer vision pipeline.
* 
* NOTE: Methods take in a BufferedImage and return a BufferedImage.
*/
class ImageProcess{

  /**   EROSIONS
  *     RETRURNS THE IMAGE UPDATED
  *
  */
  public BufferedImage erosion(BufferedImage in){
    BufferedImage out = copyBI(in);

    int[][] neighbours = new int[][]{ {0,-1}, {-1,0}, {1,0}, {0,1} };

    int inWidth = in.getWidth();
    int inHeight = in.getHeight();

    int inPixel;
    int outPixel;
    int nX;
    int nY;

    //for all pixel in the input image
    for(int y = 0; y < inHeight; y++){
      for(int x = 0; x < inWidth; x++){
        //if pixel is background ( check for white by just checking blue Channel )
        inPixel = in.getRGB(x,y) & 0xff;
        if(inPixel == 255){
          //for all neighbours of pixel
          for(int i = 0; i < 4; i++){
            nX = x + neighbours[i][0];
            nY = y + neighbours[i][1];
            //if neighbouring pixel is inside image limits
            if(nX > 0 && nX < inWidth && nY > 0 && nY < inHeight){
              outPixel = out.getRGB(nX,nY) & 0xff;
              if(outPixel == 0){
                out.setRGB(nX, nY, -1);
              }
            }
          }
        }
      }
    }
    return out;
  }


  /**  DILATION
  *    RETURNS THE IMAGE UPDATED
  *
  */
  public BufferedImage dilation(BufferedImage in){
    BufferedImage out = copyBI(in);

    int[][] neighbours = new int[][]{ {0,-1}, {-1,0}, {1,0}, {0,1} };

    int inWidth = in.getWidth();
    int inHeight = in.getHeight();

    int inPixel;
    int outPixel;
    int nX;
    int nY;

    //for all pixel in the input image
    for(int y = 0; y < inHeight; y++){
      for(int x = 0; x < inWidth; x++){
        //if pixel is foreground
        inPixel = in.getRGB(x,y) & 0xff;
        if(inPixel == 0){
          //for all neighbours of pixel
          for(int i = 0; i < 4; i++){
            nX = x + neighbours[i][0];
            nY = y + neighbours[i][1];
            //if neighbouring pixel is inside image limits
            if(nX > 0 && nX < inWidth && nY > 0 && nY < inHeight){
              outPixel = out.getRGB(nX,nY) & 0xff;
              if(outPixel == 255){
                out.setRGB(nX, nY, -16777216);
              }
            }
          }
        }
      }
    }
    return out;
  }


  /** THRESHOLDING WITH A MIN AND MAX
  *   SET ALL PIXELS WITH VALUE FOUND IN RANGE TO BLACK
  *
  */
  public BufferedImage thresholding(BufferedImage in, int min, int max){

    int inWidth = in.getWidth();
    int inHeight = in.getHeight();

    BufferedImage out = new BufferedImage(inWidth, inHeight,BufferedImage.TYPE_INT_RGB);
    int inPixel;
    for(int y = 0; y < inHeight; y++){
      for(int x = 0; x < inWidth; x++){
        inPixel = in.getRGB(x,y) & 0xff;
        if(inPixel >= min && inPixel <=max){
          out.setRGB(x,y,-16777216);
        }
        else{
          out.setRGB(x,y,-1);
        }
      }
    }
    return out;
  }


  /**   THRESHOLDING VIA A PERCENTAGE
  *     COUNTS PIXELS FROM WHTIE TO BLACK FINDS THRESHOLD CLOSEST TO PERCENTAGE
  */
  public BufferedImage thresholding(BufferedImage in,double percent){

    HistData hd = rawHistogram(in);

    double total = (double)hd.count;
    double pixels = 0.00;
    int threshold = 255;
    for(int i = hd.hist.length-1; i >= 0; i--){
      if(percent >= pixels * 100/ total){
        if(percent < pixels + (double)hd.hist[i] * 100/total){
            //return the index that result in CLOSEST (pixels out of total) to the percentage wanted
            if((pixels+hd.hist[i]-percent) < (percent-pixels)){
              threshold=i;
            }else{
              threshold=i-1;
            }
        }
      }
    }

    int inWidth = in.getWidth();
    int inHeight = in.getHeight();

    BufferedImage out = new BufferedImage(inWidth, inHeight,BufferedImage.TYPE_INT_RGB);
    int inPixel;
    for(int y = 0; y < inHeight; y++){
      for(int x = 0; x < inWidth; x++){
        inPixel = in.getRGB(x,y) & 0xff;
        if(inPixel >= threshold && inPixel <=255){
          out.setRGB(x,y,-16777216);
        }
        else{
          out.setRGB(x,y,-1);
        }
      }
    }
    return out;
  }


  /** HISTOGRAM
  *   RETRURNS AN IMAGE THAT IS A HISTOGRAM
  */
  public BufferedImage histogram(BufferedImage in){

    HistData hd = rawHistogram(in);

    int padd = 50;
    int histWidth = 4096;

    //squished the histogram and stretch it out (just for visulation)
    int squishedHeight = (int)Math.ceil((double)hd.max/4.0);

    int[] squishedHist = new int[256];
    for(int i = 0; i < 256; i++){
      squishedHist[i] = (int)Math.ceil((double)hd.hist[i]/4.0);
    }

    //create out image set pixels to white (for png use)
    BufferedImage out = new BufferedImage( histWidth + padd, squishedHeight + padd ,BufferedImage.TYPE_INT_RGB);
    for(int y = 0; y < squishedHeight + padd; y++){
      for(int x = 0; x < histWidth + padd; x++){
        out.setRGB(x,y,-1);
      }
    }
    //add hist to out make each bar 8pixels wide
    for(int x = 0; x < squishedHist.length; x++){
      for(int y = 0; y < squishedHist[x]; y++){
        int xx = (x*8);
        for(int i = 0; i < 8; i++){
          out.setRGB(xx+i+(padd/2),squishedHeight-y+(padd/2),-16777216);
        }
      }
    }
    return out;
  }
  /**  RAWHISTGRAM
  *    RETURNS THE HISTOGRAM DATA HISTDATA CLASS
  */
  private HistData rawHistogram(BufferedImage in){

    int inWidth = in.getWidth();
    int inHeight = in.getHeight();

    int inPixel;

    int count = 0;
    int maxHeight = 0;
    int histogram[] = new int[256];

    //for all pixel in input image
    for(int y = 0; y < inHeight; y++){
      for(int x = 0; x < inWidth; x++){
        //increment counter as index of histogram using pixel color as index
        inPixel = in.getRGB(x,y) & 0xff;
        histogram[inPixel] ++;
        //get the max value in histogram
        if(histogram[inPixel]>maxHeight)
          maxHeight = histogram[inPixel];
        //counter
        count++;
      }
    }
    return new HistData(maxHeight, count, histogram);
  }


  /** REGIONDETECTION
  *   COUNTS THE REGIONS IN IMAGE (BLK&WHITE)
  */
  public int regionDetection(BufferedImage in){

    int inWidth = in.getWidth();
    int inHeight = in.getHeight();

    int[][] binaryMask = new int[inWidth][inHeight];
    //initlalize binaryMask from input black = 1 white = 0
    for(int y = 0; y < inHeight; y++){
      for(int x = 0; x < inWidth; x++){
        int pixel = in.getRGB(x,y) & 0xff;
        if(pixel == 255){
          binaryMask[x][y] = 0;
        }
        else{
          binaryMask[x][y] = 1;
        }
      }
    }
    //run region detection
    int label = 2;
    int count = 0;
     for(int y = 0; y < binaryMask[0].length; y++){
       for(int x = 0; x < binaryMask.length; x++){
         if(1==binaryMask[x][y]){
           //floodfill(depth first)
           binaryMask = floodfill(x,y, label, binaryMask);
           label++;
           count++;
         }
       }
     }
     System.out.println("count : "+count+" lable : "+label);
     return count;
  }

  private int[][] floodfill( int x, int y, int label, int[][] binaryMask){
    Deque<Point> Stack = new LinkedList<Point>();
    Stack.push(new Point(x,y));
    while(!Stack.isEmpty()){
      Point p = Stack.pop();
      int px = (int)p.getX();
      int py = (int)p.getY();

      if( px >= 0 && px < binaryMask.length && py >= 0 && py < binaryMask[0].length){
        if(1==binaryMask[px][py]){
          binaryMask[px][py]=label;
          Stack.push(new Point(px-1,py ));
          Stack.push(new Point(px ,py-1));
          Stack.push(new Point(px+1,py ));
          Stack.push(new Point(px ,py+1));
        }
      }
    }
    return binaryMask;
  }


  /** COPYBI
  *   Returns a duplicates of the bufferedImage 
  */
  private static BufferedImage copyBI(BufferedImage in){
    //wont work on a subimage
    ColorModel cm = in.getColorModel();
    boolean iam = cm.isAlphaPremultiplied();
    WritableRaster raster = in.copyData(null);
    return new BufferedImage(cm, raster, iam, null);
  }
}


class HistData{
  int max;
  int count;
  int[] hist;
  public HistData(int max, int count, int[] hist){
    this.max = max;
    this.count = count;
    this.hist = hist;
  }
}
