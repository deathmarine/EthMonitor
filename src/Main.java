import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.UIManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main implements Runnable{
	static String OVERALL_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstat1\"}\r\n";
	static String DETAILED_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstathr\"}\r\n"; //Crashes Ethminer on linux
	static String RESTART = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_restart\"}\r\n"; //Crashes Ethminer on linux
	static String REBOOT = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_reboot\"}\r\n"; //Does Nothing
	
	

    JSONParser parser = new JSONParser();
	StatusWindow window;
	static boolean RUNNING = true;
	List<Double> rate_history = new ArrayList<Double>();
	
	boolean animate = true;
	int verbose = 0;
	int poling_rate = 1000;
	Set<Server> servers = new HashSet<Server>(); 
	
	public Main(String[] args) {
		if(args.length > 0) {
			
		}else {
			File config = new File("config.ini");
			if(!config.exists()) {
				try {
					System.out.println("[EthMonitor] No Configuration found, generating config.ini");
					config.createNewFile();
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config)));
					bw.write("#IPaddress and port of the server to pole, more than one server line can be added");
					bw.newLine();
					bw.write("#Example: server={ipaddress}:{port}");
					bw.newLine();
					bw.write("server=127.0.0.1:3333");
					bw.newLine();
					bw.write("#Poling rate, amount of time in ms to wait between poles");
					bw.newLine();
					bw.write("poling_rate=1000");
					bw.newLine();
					bw.write("#Verbosity of the console, 1=TX/RX info, 2=ResponseParsing");
					bw.newLine();
					bw.write("verbose=0");
					bw.newLine();
					bw.write("#Animate gauges, 1=true (default), 0=false");
					bw.newLine();
					bw.write("animate=0");
					bw.newLine();
					bw.close();				
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
				String line;
				while((line = br.readLine()) != null){
					if(!line.startsWith("#") && line.contains("=")) {
						String[] kv = line.split("=");
						if(kv[0].equalsIgnoreCase("server")) {
							if(kv[1].contains(":")) {
								String[] ip_port = kv[1].split(":");
								servers.add(new Server(ip_port[0], Integer.parseInt(ip_port[1])));
							}else {
								servers.add(new Server(kv[1], 3333));						
							}
						}
						if(kv[0].equalsIgnoreCase("poling_rate")) {
							poling_rate = Integer.parseInt(kv[1]);
						}
						if(kv[0].equalsIgnoreCase("verbose")) {
							verbose = Integer.parseInt(kv[1]);
						}	
						if(kv[0].equalsIgnoreCase("animate")) {
							animate = kv[1].equalsIgnoreCase("true");
						}						
					}					
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}		
	}
	
	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		//SwingUtilities.invokeLater(new Main(args));
		Main main = new Main(args);
		new Thread(main).start();
	}
	
	@Override
	public void run() {
		
		long time = System.currentTimeMillis();
		//boolean flip = true;
		boolean once = true;
		
		try {
			while(RUNNING) {
				if(System.currentTimeMillis() - time >= 1000) {
					time = System.currentTimeMillis();
					double total_hashrate = 0;
					double avg_temp = 0;
					double avg_fan = 0;
					double avg_watt = 0; //TODO: When Ethminer issues are resolved.
					int total_shares = 0;
					int longest_time = 0;
					double largest_share = 0;
					
					int gpu_amt = 0;
					for (Server server : servers) {
						JSONObject json_obj = (JSONObject) parser
								.parse(this.connect(server.getIPAddress(), server.getPort(), OVERALL_STATUS));
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
							if(verbose >= 2) {
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
							}
							
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
						window.avg_fan_display.setLcdValueAnimated(avg_fan/servers.size());
						window.avg_temp_display.setLcdValueAnimated(avg_temp/servers.size());
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
						//Note: Find out what to build before constructing the window.
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
		if(verbose >= 1)
			System.out.print("[Client] Sending: " + command);
		bw.write(command);
		bw.flush();
		BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		String line = br.readLine();
		if(verbose >= 1) {
			System.out.print("[Client] Receiving: ");
			System.out.println(line);			
		}
		bw.close();
		br.close();
		sock.close();
		return line;
	}
}
