package mbpmcsn.csv;

public final class CsvWriterInvokeException extends RuntimeException {
    private final Throwable t;

    public CsvWriterInvokeException(Throwable t){
        this.t = t;
    }

    @Override
    public synchronized Throwable getCause() {
        return t;
    }
}
