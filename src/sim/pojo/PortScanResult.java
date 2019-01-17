package sim.pojo;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class PortScanResult {
	private int port;

    private boolean isOpen;

    public PortScanResult(int port, boolean isOpen) {
        super();
        this.port = port;
        this.isOpen = isOpen;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }
}
