import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main implements Runnable{
	static String OVERALL_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstat1\"}\r\n";
	static String DETAILED_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstathr\"}\r\n";
	static String RESTART = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_restart\"}\r\n";
	static String REBOOT = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_reboot\"}\r\n"; //Does Nothing
	
	static Server[] servers = new Server[] {new Server("192.168.50.77", 3333), new Server("127.0.0.1", 3333)}; 

    JSONParser parser = new JSONParser();
	StatusWindow window;
	static boolean RUNNING = true;
	List<Double> rate_history = new ArrayList<Double>();
	public Main() {	
	}
	
	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		new Thread(new Main()).start();
	}
	
	@Override
	public void run() {
		
		long time = System.currentTimeMillis();
		boolean flip = true;
		boolean once = true;
		
		try {
			while(RUNNING) {
				if(System.currentTimeMillis() - time >= 1000) {
					time = System.currentTimeMillis();
					double total_hashrate = 0;
					double avg_temp = 0;
					double avg_fan = 0;
					double avg_watt = 0;
					int total_shares = 0;
					int longest_time = 0;
					double largest_share = 0;
					
					int gpu_amt = 0;
					for (Server server : servers) {
						//System.out.println("Server " + server.ip_address);
						JSONObject json_obj = (JSONObject) parser
								.parse(this.connect(server.ip_address, server.port, OVERALL_STATUS));
						Object obj = json_obj.get("result");
						if (obj instanceof JSONArray) {
							JSONArray jarray = (JSONArray) obj;
							StatusOne status = new StatusOne(jarray);
							if (window != null) {
								for (int i = gpu_amt; i < status.getAmtGPUs()+gpu_amt; i++) {									
									window.gpu_hashrate.get(i).setValueAnimated(status.getGPURate(i-gpu_amt));
									window.gpu_fan.get(i).setLcdValueAnimated(status.getSpecificFan(i-gpu_amt));
									window.gpu_temp.get(i).setLcdValueAnimated(status.getSpecificTemp(i-gpu_amt));
								}
								if(window.total_hashrate.isLedBlinking()) {
									window.total_hashrate.setLedBlinking(false);
								}
								if(!window.isShowing()) {
									System.exit(0);
								}
							}
							/*
							System.out.println("Hashrate: " + status.getHashrate() + "Mh/s");
							for (int i = 0; i < status.getAmtGPUs(); i++) {
								System.out.print("G" + i + ": " + status.getGPURate(i) + "Mh/s  ");
							}
							System.out.println();
							for (int i = 0; i < status.getAmtGPUs(); i++) {
								System.out.print("F" + i + ": " + status.getSpecificFan(i) + "%         ");
							}
							System.out.println("Avg: " + status.getAvgFan() + "%");
							for (int i = 0; i < status.getAmtGPUs(); i++) {
								System.out.print("T" + i + ": " + status.getSpecificTemp(i) + "C         ");
							}
							System.out.println("Avg: " + status.getAvgTemp() + "C");
							System.out.println("Sharerate: " + status.getSharesPerMin() + " S/min");
							System.out.println();
							*/
							total_hashrate += status.getHashrate();
							total_shares += status.getShares();
							avg_fan += status.getAvgFan();
							avg_temp += status.getAvgTemp();
							gpu_amt += status.getAmtGPUs();
							/*
							if(status.getHashrate() < 1) {
								this.connect(server.ip_address, server.port, RESTART);
							}
							*/
							if(longest_time < status.getRuntime()) {
								longest_time = status.getRuntime();
							}
							if(largest_share < status.getSharesPerMin()) {
								largest_share = status.getSharesPerMin();
							}
						}

					}

					if(window != null) {
						window.total_hashrate.setValueAnimated(total_hashrate);
						window.avg_fan_display.setLcdValueAnimated(avg_fan/servers.length);
						window.avg_temp_display.setLcdValueAnimated(avg_temp/servers.length);
						rate_history.add(total_hashrate);
						if(rate_history.size() > 100) {
							rate_history.remove(0);
						}
						window.graph.setScores(rate_history);
						window.running_time.setLcdValueAnimated(longest_time);
						window.shares.setLcdValueAnimated(total_shares);
						window.shares_per_min.setLcdValueAnimated(largest_share);
					}
					if(once) {
						once = false;
						window = new StatusWindow(this, gpu_amt);						
					}
				}
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}		
	}
	public String connect(String ip_address, int port, String command) throws UnknownHostException, IOException, ParseException {
		Socket sock = new Socket(InetAddress.getByName(ip_address), port);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		//System.out.print("[Client] Sending: " + command);
		bw.write(command);
		bw.flush();
		BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		String line = br.readLine();
		//System.out.print("[Client] Receiving: ");
		//System.out.println(line);
		bw.close();
		br.close();
		sock.close();
		return line;
	}
}
