package mbpmcsn.csv;

public final class CsvWriterException extends Exception {
    private final String message;

    public CsvWriterException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;    
    } 
}
