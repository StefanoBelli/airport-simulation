package mbpmcsn.desbook;

import java.io.*;
import java.util.*;
import java.text.*;

class Acs {

  static int K    = 50;               /* K is the maximum lag          */
  static int SIZE = K + 1;

  public static void main(String[] args) throws IOException {

    int    i = 0;                   /* data point index              */
    int    j;                       /* lag index                     */
    int    p = 0;                   /* points to the head of 'hold'  */
    double x;                       /* current x[i] data point       */
    double sum = 0.0;               /* sums x[i]                     */
    long   n;                       /* number of data points         */
    double mean;
    double hold[]  = new double [SIZE]; /* K + 1 most recent data points */
    double cosum[] = new double [SIZE]; /* cosum[j] sums x[i] * x[i+j]   */

    for (j = 0; j < SIZE; j++)
      cosum[j] = 0.0;

    String line;
    InputStreamReader r = new InputStreamReader(System.in);
    BufferedReader ReadThis = new BufferedReader(r);
    try {                         /* the first K + 1 data values    */
      while (i < SIZE) {              /* initialize the hold array with */
        if ( (line = ReadThis.readLine()) != null) {
          x        = Double.parseDouble(line);
          sum     += x;
          hold[i]  = x;
          i++;
        }
      }

      while ( (line = ReadThis.readLine()) != null ) {
        for (j = 0; j < SIZE; j++)
          cosum[j] += hold[p] * hold[(p + j) % SIZE];
        x       = Double.parseDouble(line);
        sum    += x;
        hold[p] = x;
        p       = (p + 1) % SIZE;
        i++;
      }
    } catch (EOFException e) {
      System.out.println("Acs: " + e);
    } catch (NumberFormatException nfe) {
//      System.out.println("Acs: " + nfe);
    }

    n = i;
    while (i < n + SIZE) {        /* empty the circular array       */
      for (j = 0; j < SIZE; j++)
        cosum[j] += hold[p] * hold[(p + j) % SIZE];
      hold[p] = 0.0;
      p       = (p + 1) % SIZE;
      i++;
    }

    mean = sum / n;
    for (j = 0; j <= K; j++)
      cosum[j] = (cosum[j] / (n - j)) - (mean * mean);

    DecimalFormat f = new DecimalFormat("###0.00");
    DecimalFormat g = new DecimalFormat("###0.000");

    System.out.println("for " + n + " data points");
    System.out.println("the mean is ... " + f.format(mean));
    System.out.println("the stdev is .. " + f.format(Math.sqrt(cosum[0])) +"\n");
    System.out.println("  j (lag)   r[j] (autocorrelation)");
    for (j = 1; j < SIZE; j++)
      System.out.println("  " + j + "          " + g.format(cosum[j] / cosum[0]));
  }
}
