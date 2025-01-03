package connectionpool;

public class Connection {
    private final String id;
    private boolean isValid;

    Connection(String id) {
        this.id = id;
        this.isValid = true;
    }

    public boolean isValid() {
        return isValid;
    }

    public void close() {
        isValid = false;
    }
}
