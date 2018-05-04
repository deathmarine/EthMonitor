package no.url.ethmonitor;

public class Server {
	private String IP_ADDRESS;
	private int PORT;
	public Server(String ip_address, int port) {
		this.setIPAddress(ip_address);
		this.setPort(port);
	}
	public String getIPAddress() {
		return IP_ADDRESS;
	}
	public void setIPAddress(String iP_ADDRESS) {
		IP_ADDRESS = iP_ADDRESS;
	}
	public int getPort() {
		return PORT;
	}
	public void setPort(int pORT) {
		PORT = pORT;
	}
}
