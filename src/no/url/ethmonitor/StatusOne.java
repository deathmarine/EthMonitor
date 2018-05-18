package no.url.ethmonitor;

import org.json.simple.JSONArray;

public class StatusOne extends Status{
	private JSONArray array;
	/*
	response[0] = ethminer_get_buildinfo()->project_version;  //miner version.
	response[1] = toString(runningTime.count()); // running time, in minutes.
	response[2] = totalMhEth.str();              // total ETH hashrate in MH/s, number of ETH shares, number of ETH rejected shares.
	response[3] = detailedMhEth.str();           // detailed ETH hashrate for all GPUs.
	response[4] = totalMhDcr.str();              // total DCR hashrate in MH/s, number of DCR shares, number of DCR rejected shares.
	response[5] = detailedMhDcr.str();           // detailed DCR hashrate for all GPUs.
	response[6] = tempAndFans.str();             // Temperature and Fan speed(%) pairs for all GPUs.
	response[7] = poolAddresses.str();           // current mining pool. For dual mode, there will be two pools here.
	response[8] = invalidStats.str();            // number of ETH invalid shares, number of ETH pool switches, number of DCR invalid shares, number of DCR pool switches.
	 */
	/**
	 * 
	 * @param array
	 */
	public StatusOne(JSONArray array) {
		this.setJSONArray(array);
		this.setVersion((String) array.get(0));
		this.setRuntime(Integer.parseInt((String) array.get(1)));

		String[] total_eth = ((String) array.get(2)).split(";");
		this.setHashrate(Float.valueOf(total_eth[0]) / 1000);
		this.setShares(Integer.parseInt(total_eth[1]));
		this.setInvalid(Integer.parseInt(total_eth[2]));
		int c = 0;
		for (String s : ((String) array.get(6)).split(" ")) {
			c++;
			String[] tuple = s.split(";");
			AVG_TEMP += Double.parseDouble(tuple[0]);
			AVG_FAN += Double.parseDouble(tuple[1]);
		}
		AVG_TEMP = AVG_TEMP / c;
		AVG_FAN = AVG_FAN / c;
		this.setPool((String) array.get(7));

	}

	public JSONArray getJSONArray() {
		return array;
	}

	public void setJSONArray(JSONArray array) {
		this.array = array;
	}

	public int getSpecificTemp(int gpu) {
		return Integer.parseInt(((String) array.get(6)).split(" ")[gpu].split(";")[0]);
	}

	public int getSpecificFan(int gpu) {
		return Integer.parseInt(((String) array.get(6)).split(" ")[gpu].split(";")[1]);
	}

	public double getSharesPerMin() {
		return ((double) this.getShares()) / (double) this.getRuntime();
	}

	public int getAmtGPUs() {
		return ((String) array.get(3)).split(";").length;
	}

	public double getGPURate(int gpu) {
		return Double.parseDouble(((String) array.get(3)).split(";")[gpu]) / 1000;
	}

	public String toString() {
		return array.toString();
	}
}