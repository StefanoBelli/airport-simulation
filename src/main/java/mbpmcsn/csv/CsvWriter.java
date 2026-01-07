package mbpmcsn.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import mbpmcsn.csv.annotations.CsvColumn;
import mbpmcsn.csv.annotations.CsvDescriptor;

/*
 * How to use this?
 *
 *  1) Declare a class using CsvDescriptor annotation
 *
 * @CsvDescriptor
 * public final class Release { ...
 *
 *  2) Inside the CSV descriptor class, declare columns in
 *  whatever order you'd like. Methods may be named however you want.
 *
 * @CsvColumn(order = 1, name = "Index")
 * public int getIndex() {
 *     return index;
 * }
 *
 * @CsvColumn(order = 2, name = "Version ID")
 * public String getId() {
 *     return id;
 * }
 *
 *  3) Use the class normally - set its fields via setters for example
 * 
 * Release rel = new Release();
 * rel.setIndex(10);
 * rel.setId("release name");
 *
 *  4) Do this for each data row of the CSV file
 *  
 *  5) Create a List<> of these instances which represent rows in the CSV
 *
 * List<Release> relCsvRows = new ArrayList<>();
 * relCsvRows.add(...);
 * relCsvRows.add(...);
 *
 *  6) Write everything to file
 *
 * CsvWriter.writeAll("output.csv", Release.class, relCsvRows);
 *
 *  7) Done
 */

class Threeple<I,J,K> {
      private final I i;
      private final J j;
      private final K k;

      public Threeple(I i, J j, K k) {
         this.i = i;
         this.j = j;
         this.k = k;
      }

      public I getFirst() {
          return i;
      }

      public J getSecond() {
          return j;
      }

      public K getThird() {
          return k;
      }
}

public final class CsvWriter {
    private final StringBuilder csvBuilder;
    private final List<Method> valueGetters;
    private final String filename;

    private CsvWriter(String filename, Class<?> c) throws CsvWriterException {
        if(!c.isAnnotationPresent(CsvDescriptor.class)) {
            throw new CsvWriterException("Class must have @CsvDescriptor annotation");
        }

        List<Threeple<Integer, String, Method>> intermediate 
            = new ArrayList<>();

        for(Method m : c.getMethods()) {
            CsvColumn a = m.getAnnotation(CsvColumn.class);
            if(a != null) {
                intermediate.add(new Threeple<>(a.order(), a.name(), m));
            }
        }

        intermediate.sort((o1, o2) -> o1.getFirst() - o2.getFirst());

        valueGetters = new ArrayList<>();
        csvBuilder = new StringBuilder();

        int sz = intermediate.size();

        for(int i = 0; i < sz; ++i) {
            Threeple<Integer, String, Method> tmp = intermediate.get(i);
            valueGetters.add(tmp.getThird());
            csvBuilder
                .append(tmp.getSecond())
                .append(i == sz - 1 ? "\n" : ",");
        }

        this.filename = filename;
    }

    private void addEntry(Object any) {
        int nMethods = valueGetters.size();
        for(int i = 0; i < nMethods; ++i) {
            try {
                csvBuilder
                    .append(valueGetters.get(i).invoke(any))
                    .append(i == nMethods - 1 ? '\n' : ',');
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new CsvWriterInvokeException(e);
            }
        }
    }

    private void write() 
            throws IOException {

        File f = new File(filename);
        f.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(csvBuilder.toString().getBytes());
            fos.flush();
        }
    }


    public static <T> void writeAll(String filename, Class<T> cls, List<T> elems) 
            throws CsvWriterException, IOException {
        CsvWriter csv = new CsvWriter(filename, cls);
        for(T elem : elems) {
            csv.addEntry(elem);
        }
        csv.write();
    }
}
