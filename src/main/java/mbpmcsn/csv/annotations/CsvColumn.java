package mbpmcsn.csv.annotations;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface CsvColumn {
    public int order();
    public String name();
}
