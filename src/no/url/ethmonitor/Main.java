package no.url.ethmonitor;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.text.DefaultEditorKit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main implements Runnable {
	// Working
	static String OVERALL_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstat1\"}\r\n";
	static String DETAILED_STATUS = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_getstathr\"}\r\n";
	// Do Not Use, Crashes Etherminer
	static String RESTART = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_restart\"}\r\n";
	// Do Not Use, Does Nothing
	static String REBOOT = "{\"id\":0,\"jsonrpc\":\"2.0\",\"method\": \"miner_reboot\"}\r\n";
	
	static boolean RUNNING = true;
	static boolean DISCONNECTED = false;
	static boolean RECONNECT = false;
	JSONParser parser = new JSONParser();
	
	StatusWindow window;

	// Maybe this would be a good time to consider databasing
	List<Status> main_history = new ArrayList<Status>();
	List<Double> main_temp_history = new ArrayList<Double>();
	List<Double> main_fan_history = new ArrayList<Double>();
	List<Double> main_watt_history = new ArrayList<Double>();
	List<Double> main_share_per_seg = new ArrayList<Double>();

	// Settings
	CheckboxMenuItem animate = new CheckboxMenuItem("Animate");
	boolean tray = true;
	boolean tray_question = true;
	boolean detailed_result = false;
	int verbose = 0;
	int poling_rate = 1000;
	int graph_points = 100;
	int gauge_max_status = 200; // Should autoscale but meh
	int gauge_max_gpu = 50; // Should autoscale but meh

	Set<Server> servers = new HashSet<Server>();

	double count_shares = 0; // ten min interval count. Let's see if this can match the pool
	boolean count_reset = false;
	

	private Socket sock; //Keep socket open.
	private BufferedWriter bw; //Closing the writer terminates the socket
	private BufferedReader br;
	

	public Main(String[] args) {
		if (args.length > 0) {

		} else {
			File config = new File("config.ini");
			if (!config.exists()) {
				try {
					System.out.println("[EthMonitor] No Configuration found, generating config.ini");
					config.createNewFile();
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config)));
					bw.write("## Configuration ##\r\n"
							+ "#IPaddress and port of the server to pole, more than one server line can be added\r\n"
							+ "#Example: server={ipaddress}:{port}\r\n" + "server=127.0.0.1:3333\r\n\r\n"
							+ "#Enable Tray Icon, true (default), false\r\n" + "trayicon=true\r\n"
							+ "#Enable \"AreYouSure\" Question for exiting.\r\n" + "trayicon.question=true\r\n"
							+ "#Detailed results, includes wattage\r\n" + "detailed=true\r\n\r\n"
							+ "##   Appearance   ##\r\n" + "#Max hashrate, status gauge (default:200)\r\n"
							+ "gauge_max.status=200\r\n" + "#Max hashrate, gpu gauge (default:50)\r\n"
							+ "gauge_max.gpu=50\r\n" + "#Poling rate, amount of time in ms to wait between poles\r\n"
							+ "poling_rate=1000\r\n" + "#Graphing Points (default:100)\r\n" + "graph_points=100\r\n"
							+ "#Verbosity of the console, 1=TX/RX info, 2=ResponseParsing\r\n" + "verbose=0\r\n"
							+ "#Animate gauges, true (default), false\r\n" + "animate=true\r\n");
					bw.close();
				} catch (IOException e) {
					Main.showExceptionDialog("Error", e);
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
						case "gauge_max.status":
							gauge_max_status = Integer.parseInt(kv[1].trim());
						case "gauge_max.gpu":
							gauge_max_gpu = Integer.parseInt(kv[1].trim());
						case "poling_rate":
							poling_rate = Integer.parseInt(kv[1].trim());
						case "graph_points":
							graph_points = Integer.parseInt(kv[1].trim());
						case "verbose":
							verbose = Integer.parseInt(kv[1].trim());
						case "animate":
							animate.setState(kv[1].trim().equalsIgnoreCase("true"));
						case "trayicon":
							tray = kv[1].trim().equalsIgnoreCase("true");
						case "trayicon.question":
							tray_question = kv[1].trim().equalsIgnoreCase("true");
						case "detailed":
							detailed_result = kv[1].trim().equalsIgnoreCase("true");
						}
					}
				}
				br.close();
			} catch (IOException e) {
				Main.showExceptionDialog("Error", e);
				e.printStackTrace();
			}
		}
		if (tray) {
			if (!SystemTray.isSupported()) {
				System.out.println("SystemTray is not supported");
				return;
			} else {
				PopupMenu popup = new PopupMenu();
				Image image = Toolkit.getDefaultToolkit()
						.getImage(this.getClass().getResource("/resources/icon16.png"));
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
				 * Menu displayMenu = new Menu("Display"); MenuItem errorItem = new
				 * MenuItem("Error"); MenuItem warningItem = new MenuItem("Warning"); MenuItem
				 * infoItem = new MenuItem("Info"); MenuItem noneItem = new MenuItem("None");
				 * popup.add(displayMenu); displayMenu.add(errorItem);
				 * displayMenu.add(warningItem); displayMenu.add(infoItem);
				 * displayMenu.add(noneItem);
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
					Main.showExceptionDialog("Error", e);
					System.out.println("TrayIcon could not be added.");
				}
			}
		}
	}

	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			// SwingUtilities.invokeLater(new Main(args));
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					new Thread(new Main(args)).start();					
				}
				
			});
		} catch (Exception e) {
			Main.showExceptionDialog("Error", e);
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		long time = System.currentTimeMillis();
		boolean once = true;
		int shares = 0;

		try {
			while (RUNNING) {
				if (System.currentTimeMillis() - time >= poling_rate) {
					time = System.currentTimeMillis();
					float total_hashrate = 0;
					double avg_temp = 0;
					double avg_fan = 0;
					double total_watt = 0; // TODO: When Ethminer issues are resolved.
					int total_shares = 0;
					int longest_time = 0;
					double largest_share = 0;

					int gpu_amt = 0;
					for (Server server : servers) {
						if (detailed_result) {
							String data = this.connect(server.getIPAddress(), server.getPort(), DETAILED_STATUS);
							if(data == null)
								continue;
							JSONObject json_obj = (JSONObject) parser.parse(data);
							if (json_obj != null) {
								Object obj = json_obj.get("result");
								if (obj instanceof JSONObject) {
									StatusHR status = new StatusHR((JSONObject) obj);
									main_history.add(status);

									if (window != null) {
										for (int i = gpu_amt; i < status.getAmtGPUs() + gpu_amt; i++) {
											if (animate.getState()) {
												window.gpu_hashrate.get(i)
														.setValueAnimated(status.getGPURate(i - gpu_amt));
												window.gpu_fan.get(i)
														.setLcdValueAnimated(status.getSpecificFan(i - gpu_amt));
												window.gpu_watt.get(i)
														.setLcdValueAnimated(status.getSpecificPower(i - gpu_amt));
												window.gpu_temp.get(i)
														.setLcdValueAnimated(status.getSpecificTemp(i - gpu_amt));
											} else {
												window.gpu_hashrate.get(i).setValue(status.getGPURate(i - gpu_amt));
												window.gpu_fan.get(i).setLcdValue(status.getSpecificFan(i - gpu_amt));
												window.gpu_watt.get(i)
														.setLcdValue(status.getSpecificPower(i - gpu_amt));
												window.gpu_temp.get(i).setLcdValue(status.getSpecificTemp(i - gpu_amt));
											}

											ArrayList<Double> hashrate = new ArrayList<Double>();
											for (Object obj1 : main_history) {
												if (obj1 instanceof StatusHR)
													hashrate.add((double) ((StatusHR) obj1).getGPURate(i));
												if (obj1 instanceof StatusOne)
													hashrate.add((double) ((StatusOne) obj1).getGPURate(i));
											}
											window.gpu_hashrate_graph.get(i).setScores(hashrate);

											ArrayList<Double> temperature = new ArrayList<Double>();
											for (Object obj1 : main_history) {
												if (obj1 instanceof StatusHR)
													temperature.add((double) ((StatusHR) obj1).getSpecificTemp(i));
												if (obj1 instanceof StatusOne)
													temperature.add((double) ((StatusOne) obj1).getSpecificTemp(i));
											}
											window.gpu_temperature_graph.get(i).setScores(temperature);

											ArrayList<Double> fan = new ArrayList<Double>();
											for (Object obj1 : main_history) {
												if (obj1 instanceof StatusHR)
													fan.add((double) ((StatusHR) obj1).getSpecificFan(i));
												if (obj1 instanceof StatusOne)
													fan.add((double) ((StatusOne) obj1).getSpecificFan(i));
											}
											window.gpu_fan_graph.get(i).setScores(fan);

											ArrayList<Double> wattage = new ArrayList<Double>();
											for (Object obj1 : main_history) {
												if (obj1 instanceof StatusHR)
													wattage.add((double) ((StatusHR) obj1).getSpecificPower(i));
											}
											window.gpu_wattage_graph.get(i).setScores(wattage);
										}
									}
									if (verbose >= 3) {
										System.out.println("Hashrate: " + status.getHashrate() + "Mh/s");
										for (int i = 0; i < status.getAmtGPUs(); i++) {
											System.out.print("G" + i + ": " + status.getGPURate(i) + "Mh/s  ");
										}
										System.out.println();
										for (int i = 0; i < status.getAmtGPUs(); i++) {
											System.out
													.print("W" + i + ": " + status.getSpecificPower(i) + "Watt      ");
										}
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
									total_watt += status.getTotalPower();
									avg_temp += status.getAvgTemp();
									gpu_amt += status.getAmtGPUs();

									/*
									 * 
									 * if(status.getHashrate() < 1) { this.connect(server.ip_address, server.port,
									 * RESTART); }
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
						} else {

							JSONObject json_obj = (JSONObject) parser
									.parse(this.connect(server.getIPAddress(), server.getPort(), OVERALL_STATUS));
							if (json_obj != null) {
								Object obj = json_obj.get("result");
								if (obj instanceof JSONArray) {
									JSONArray jarray = (JSONArray) obj;
									StatusOne status = new StatusOne(jarray);
									main_history.add(status);
									if (window != null) {
										for (int i = gpu_amt; i < status.getAmtGPUs() + gpu_amt; i++) {

											if (animate.getState()) {
												window.gpu_hashrate.get(i)
														.setValueAnimated(status.getGPURate(i - gpu_amt));
												window.gpu_fan.get(i)
														.setLcdValueAnimated(status.getSpecificFan(i - gpu_amt));
												window.gpu_temp.get(i)
														.setLcdValueAnimated(status.getSpecificTemp(i - gpu_amt));
											} else {
												window.gpu_hashrate.get(i).setValue(status.getGPURate(i - gpu_amt));
												window.gpu_fan.get(i).setLcdValue(status.getSpecificFan(i - gpu_amt));
												window.gpu_temp.get(i).setLcdValue(status.getSpecificTemp(i - gpu_amt));
											}

											ArrayList<Double> hashrate = new ArrayList<Double>();
											for (Object obj1 : main_history) {
												if (obj1 instanceof StatusHR)
													hashrate.add((double) ((StatusHR) obj1).getGPURate(i));
												if (obj1 instanceof StatusOne)
													hashrate.add((double) ((StatusOne) obj1).getGPURate(i));
											}
											window.gpu_hashrate_graph.get(i).setScores(hashrate);

											ArrayList<Double> temperature = new ArrayList<Double>();
											for (Object obj1 : main_history) {
												if (obj1 instanceof StatusHR)
													temperature.add((double) ((StatusHR) obj1).getSpecificTemp(i));
												if (obj1 instanceof StatusOne)
													temperature.add((double) ((StatusOne) obj1).getSpecificTemp(i));
											}
											window.gpu_temperature_graph.get(i).setScores(temperature);

											ArrayList<Double> fan = new ArrayList<Double>();
											for (Object obj1 : main_history) {
												if (obj1 instanceof StatusHR)
													fan.add((double) ((StatusHR) obj1).getSpecificFan(i));
												if (obj1 instanceof StatusOne)
													fan.add((double) ((StatusOne) obj1).getSpecificFan(i));
											}
											window.gpu_fan_graph.get(i).setScores(fan);
										}
									}
									if (verbose >= 3) {
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

									if (longest_time < status.getRuntime()) {
										longest_time = status.getRuntime();
									}
									if (largest_share < status.getSharesPerMin()) {
										largest_share = status.getSharesPerMin();
									}
								}
							}
						}
						if (total_shares > shares) {
							count_shares += total_shares - shares;
							if (verbose >= 2)
								System.out.println("Share Found!! = " + count_shares);
						}
						shares = total_shares;
					}
					if (window != null) {
						if (!window.isShowing()) {
							System.exit(0);
						}
						if (animate.getState()) {
							window.total_hashrate.setValueAnimated(total_hashrate);
							window.avg_fan_display.setLcdValueAnimated(avg_fan / servers.size());
							window.avg_temp_display.setLcdValueAnimated(avg_temp / servers.size());
							if (detailed_result)
								window.total_wattage_display.setLcdValueAnimated(total_watt);
							ArrayList<Double> list = new ArrayList<Double>();
							for (Object obj : main_history) {
								if (obj instanceof StatusHR)
									list.add((double) ((StatusHR) obj).getHashrate());
								if (obj instanceof StatusOne)
									list.add((double) ((StatusOne) obj).getHashrate());
							}
							window.main_hashrate_graph.setScores(list);

							window.running_time.setLcdValueAnimated(longest_time);
							window.shares.setLcdValueAnimated(total_shares);
							window.shares_per_min.setLcdValueAnimated(largest_share);
						} else {
							window.total_hashrate.setValue(total_hashrate);
							window.avg_fan_display.setLcdValue(avg_fan / servers.size());
							window.avg_temp_display.setLcdValue(avg_temp / servers.size());
							if (detailed_result)
								window.total_wattage_display.setLcdValue(total_watt);
							window.running_time.setLcdValue(longest_time);
							window.shares.setLcdValue(total_shares);
							window.shares_per_min.setLcdValue(largest_share);

						}
						ArrayList<Double> hashrate = new ArrayList<Double>();
						for (Object obj : main_history) {
							if (obj instanceof StatusHR)
								hashrate.add((double) ((StatusHR) obj).getHashrate());
							if (obj instanceof StatusOne)
								hashrate.add((double) ((StatusOne) obj).getHashrate());
						}
						window.main_hashrate_graph.setScores(hashrate);

						ArrayList<Double> temperature = new ArrayList<Double>();
						for (Object obj : main_history) {
							if (obj instanceof StatusHR)
								temperature.add((double) ((StatusHR) obj).getAvgTemp());
							if (obj instanceof StatusOne)
								temperature.add((double) ((StatusOne) obj).getAvgTemp());
						}
						window.main_temperature_graph.setScores(temperature);

						ArrayList<Double> fan = new ArrayList<Double>();
						for (Object obj : main_history) {
							if (obj instanceof StatusHR)
								fan.add((double) ((StatusHR) obj).getAvgFan());
							if (obj instanceof StatusOne)
								fan.add((double) ((StatusOne) obj).getAvgFan());
						}
						window.main_fan_graph.setScores(fan);

						if (detailed_result) {
							ArrayList<Double> wattage = new ArrayList<Double>();
							for (Object obj : main_history) {
								if (obj instanceof StatusHR)
									wattage.add((double) ((StatusHR) obj).getTotalPower());
							}
							window.main_wattage_graph.setScores(wattage);
						}

						if (main_history.size() > graph_points) {
							main_history.remove(0);
						}
					}
					if (once) {
						// Note: Find out what to build before constructing the window.
						once = false;
						window = new StatusWindow(this, gpu_amt);
					}
				}

				if (Calendar.getInstance().get(Calendar.MINUTE) == 0) { // Every Hour
					// if(Calendar.getInstance().get(Calendar.MINUTE) % 10 == 0 ) { //Every 10 mins
					int hour = Calendar.getInstance().get(Calendar.HOUR)-1;
					if(hour < 0) hour = 11;
					if (!count_reset) {
						count_reset = true;
						System.out.println((hour<10?"0"+hour:hour)+":00 Share Count = " + count_shares);
						main_share_per_seg.add(count_shares);
						count_shares = 0D;
					}
				} else {
					if (count_reset)
						count_reset = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Main.showExceptionDialog("Error", e);
			System.exit(0);
		}
	}
	
	public String connect(String ip_address, int port, String command)
			throws UnknownHostException {
		try {
			if(sock == null || RECONNECT) {
				if (verbose >= 1) {
					System.out.println("[Socket] Opening Socket to "+ip_address+":"+port+" !");
				}
				sock = new Socket(InetAddress.getByName(ip_address), port);
				RECONNECT = false;
			}
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		
			if (verbose >= 2)
				System.out.print("[Socket][Writer] Sending: " + command);
			bw.write(command);
			bw.flush();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String line = br.readLine();
			if (verbose >= 2) {
				System.out.print("[Socket][Reader] Receiving: ");
				System.out.println(line);
			}
			DISCONNECTED = false;
			return line;			
		} catch (IOException e) {
			if(!DISCONNECTED) {
				DISCONNECTED = true;
				RECONNECT = true;
				JOptionPane.showMessageDialog(window, "Disconnected from Server!");
				System.out.println("[Socket] Disconnected from server!");

			}

			if (verbose >= 2)
				System.out.print(e.getMessage());
		}
		//bw.close();
		//br.close();
		//sock.close();
		return "{}";
	}

	/**
	 * Method allows for users to copy the stacktrace for reporting any issues. Add
	 * Cool Hyperlink Enhanced for mouse users. Borrowed from Luyten
	 * 
	 * @param message
	 * @param e
	 */
	public static void showExceptionDialog(String message, Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String stacktrace = sw.toString();
		try {
			sw.close();
			pw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(stacktrace);

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		if (message.contains("\n")) {
			for (String s : message.split("\n")) {
				pane.add(new JLabel(s));
			}
		} else {
			pane.add(new JLabel(message));
		}
		pane.add(new JLabel(" \n")); // Whitespace
		final JTextArea exception = new JTextArea(25, 100);
		exception.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		exception.setText(stacktrace);
		exception.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					new JPopupMenu() {
						{
							JMenuItem menuitem = new JMenuItem("Select All");
							menuitem.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									exception.requestFocus();
									exception.selectAll();
								}
							});
							this.add(menuitem);
							menuitem = new JMenuItem("Copy");
							menuitem.addActionListener(new DefaultEditorKit.CopyAction());
							this.add(menuitem);
						}

						private static final long serialVersionUID = 562054483562666832L;
					}.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		JScrollPane scroll = new JScrollPane(exception);
		scroll.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Stacktrace"),
				new BevelBorder(BevelBorder.LOWERED)));
		pane.add(scroll);
		final String issue = "https://github.com/deathmarine/EthMonitor/issues";
		final JLabel link = new JLabel("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(issue));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				link.setText("<HTML>Submit to <FONT color=\"#00aa99\"><U>" + issue + "</U></FONT></HTML>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				link.setText("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
			}
		});
		pane.add(link);
		JOptionPane.showMessageDialog(null, pane, "Error!", JOptionPane.ERROR_MESSAGE);
	}
}
