package no.url.ethmonitor;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main implements Runnable {
	//Working
	static String OVERALL_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstat1\"}\r\n";
	//Do Not Use, Crashes Etherminer on Linux (Save for future iterations and AMD wattage monitoring)
	static String DETAILED_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstathr\"}\r\n";
	//Do Not Use, Crashes Etherminer
	static String RESTART = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_restart\"}\r\n";
	//Do Not Use, Does Nothing
	static String REBOOT = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_reboot\"}\r\n";

	static boolean RUNNING = true;

	JSONParser parser = new JSONParser();
	StatusWindow window;
	List<Double> rate_history = new ArrayList<Double>();
	
	//Settings
	//boolean animate = true;
	CheckboxMenuItem animate = new CheckboxMenuItem("Animate");
	boolean tray = true;
	boolean tray_question = true;
	int verbose = 0;
	int poling_rate = 1000;
	int graphing_points = 100;
	Set<Server> servers = new HashSet<Server>();

	public Main(String[] args) {
		if (args.length > 0) {

		} else {
			File config = new File("config.ini");
			if (!config.exists()) {
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
					bw.write("#Graphing Points (default:100)");
					bw.newLine();
					bw.write("graph_points=100");
					bw.newLine();
					bw.write("#Verbosity of the console, 1=TX/RX info, 2=ResponseParsing");
					bw.newLine();
					bw.write("verbose=0");
					bw.newLine();
					bw.write("#Animate gauges, 1=true (default), 0=false");
					bw.newLine();
					bw.write("animate=true");
					bw.newLine();
					bw.write("#Enable Tray Icon, 1=true (default), 0=false");
					bw.newLine();
					bw.write("trayicon=true");
					bw.newLine();
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
				String line;
				while ((line = br.readLine()) != null) {
					if (!line.startsWith("#") && line.contains("=")) {
						String[] kv = line.split("=");
						if (kv[0].equalsIgnoreCase("server")) {
							if (kv[1].contains(":")) {
								String[] ip_port = kv[1].split(":");
								servers.add(new Server(ip_port[0], Integer.parseInt(ip_port[1])));
							} else {
								servers.add(new Server(kv[1], 3333));
							}
						}
						switch (kv[0]) {
						case "poling_rate":
							poling_rate = Integer.parseInt(kv[1]);
						case "verbose":
							verbose = Integer.parseInt(kv[1]);
						case "animate":
							animate.setState(kv[1].equalsIgnoreCase("true"));
						case "trayicon":
							tray = kv[1].equalsIgnoreCase("true");
						case "trayicon.question":
							tray_question = kv[1].equalsIgnoreCase("true");
							
						}
						/*
						 * if(kv[0].equalsIgnoreCase("poling_rate")) { }
						 * if(kv[0].equalsIgnoreCase("verbose")) { verbose = Integer.parseInt(kv[1]); }
						 * if(kv[0].equalsIgnoreCase("animate")) { animate =
						 * kv[1].equalsIgnoreCase("true"); } if(kv[0].equalsIgnoreCase("trayicon")) {
						 * tray = kv[1].equalsIgnoreCase("true"); }
						 */
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		} else {
			PopupMenu popup = new PopupMenu();
			Image image = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/icon16.png"));
			TrayIcon trayIcon = new TrayIcon(image);

			SystemTray tray = SystemTray.getSystemTray();

			// Create a pop-up menu components
			MenuItem aboutItem = new MenuItem("About");
			aboutItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					JOptionPane.showMessageDialog(null, "Built by Deathmarine");
				}

			});
			popup.add(aboutItem);
			popup.addSeparator();
			
			/*
			Menu displayMenu = new Menu("Display");
			MenuItem errorItem = new MenuItem("Error");
			MenuItem warningItem = new MenuItem("Warning");
			MenuItem infoItem = new MenuItem("Info");
			MenuItem noneItem = new MenuItem("None");
			popup.add(displayMenu);
			displayMenu.add(errorItem);
			displayMenu.add(warningItem);
			displayMenu.add(infoItem);
			displayMenu.add(noneItem);
			*/
			popup.add(animate);
			popup.addSeparator();
			
			MenuItem exitItem = new MenuItem("Exit");
			exitItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Quit",
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						RUNNING = false;
						System.exit(0);
					}
				}

			});
			popup.add(exitItem);
			trayIcon.setPopupMenu(popup);

			try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				System.out.println("TrayIcon could not be added.");
			}

		}

	}

	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		// SwingUtilities.invokeLater(new Main(args));
		Main main = new Main(args);
		new Thread(main).start();
	}

	@Override
	public void run() {

		long time = System.currentTimeMillis();
		// boolean flip = true;
		boolean once = true;

		try {
			while (RUNNING) {
				if (System.currentTimeMillis() - time >= poling_rate) {
					time = System.currentTimeMillis();
					double total_hashrate = 0;
					double avg_temp = 0;
					double avg_fan = 0;
					double avg_watt = 0; // TODO: When Ethminer issues are resolved.
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
								for (int i = gpu_amt; i < status.getAmtGPUs() + gpu_amt; i++) {
									if (animate.getState()) {
										window.gpu_hashrate.get(i).setValueAnimated(status.getGPURate(i - gpu_amt));
										window.gpu_fan.get(i).setLcdValueAnimated(status.getSpecificFan(i - gpu_amt));
										window.gpu_temp.get(i).setLcdValueAnimated(status.getSpecificTemp(i - gpu_amt));
									} else {
										window.gpu_hashrate.get(i).setValue(status.getGPURate(i - gpu_amt));
										window.gpu_fan.get(i).setLcdValue(status.getSpecificFan(i - gpu_amt));
										window.gpu_temp.get(i).setLcdValue(status.getSpecificTemp(i - gpu_amt));
									}
								}
								
								if (!window.isShowing()) {
									System.exit(0);
								}
							}
							if (verbose >= 2) {
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
							 * 
							 * if(status.getHashrate() < 1) { 
							 * 		this.connect(server.ip_address, server.port, RESTART); 
							 * }
							 * 
							 */

							if (longest_time < status.getRuntime()) {
								longest_time = status.getRuntime();
							}
							if (largest_share < status.getSharesPerMin()) {
								largest_share = status.getSharesPerMin();
							}
						}

					}

					if (window != null) {
						if (animate.getState()) {
							window.total_hashrate.setValueAnimated(total_hashrate);
							window.avg_fan_display.setLcdValueAnimated(avg_fan / servers.size());
							window.avg_temp_display.setLcdValueAnimated(avg_temp / servers.size());
							rate_history.add(total_hashrate);
							window.graph.setScores(rate_history);
							window.running_time.setLcdValueAnimated(longest_time);
							window.shares.setLcdValueAnimated(total_shares);
							window.shares_per_min.setLcdValueAnimated(largest_share);
						}else {
							window.total_hashrate.setValue(total_hashrate);
							window.avg_fan_display.setLcdValue(avg_fan / servers.size());
							window.avg_temp_display.setLcdValue(avg_temp / servers.size());
							rate_history.add(total_hashrate);
							window.graph.setScores(rate_history);
							window.running_time.setLcdValue(longest_time);
							window.shares.setLcdValue(total_shares);
							window.shares_per_min.setLcdValue(largest_share);
							
						}
						if (rate_history.size() > graphing_points) {
							rate_history.remove(0);
						}
					}
					if (once) {
						// Note: Find out what to build before constructing the window.
						once = false;
						window = new StatusWindow(this, gpu_amt);
					}
				}
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	public String connect(String ip_address, int port, String command)
			throws UnknownHostException, IOException, ParseException {
		Socket sock = new Socket(InetAddress.getByName(ip_address), port);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		if (verbose >= 1)
			System.out.print("[Client] Sending: " + command);
		bw.write(command);
		bw.flush();
		BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		String line = br.readLine();
		if (verbose >= 1) {
			System.out.print("[Client] Receiving: ");
			System.out.println(line);
		}
		bw.close();
		br.close();
		sock.close();
		return line;
	}
}
