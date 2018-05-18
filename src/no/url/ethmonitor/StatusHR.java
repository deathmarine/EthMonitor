package no.url.ethmonitor;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class StatusHR{
	private JSONObject object;
	private String SOFTWARE_VERSION;
	private int RUNTIME;
	private float HASHRATE;
	private long SHARES;
	private long INVALID;
	private double AVG_TEMP = 0;
	private double AVG_FAN = 0;
	private double TOTAL_POWER = 0;
	private String POOL;
	private double[] gpu_hashrate;
	private int[] gpu_temperature;
	private int[] gpu_fan_precentage;
	private double[] gpu_power;
	
		
		//{"id":0,"jsonrpc":"2.0","result":{"ethhashrate":99923013,"ethhashrates":[29550692,11460364,29550692,29361264],"ethinvalid":0,"ethpoolsw":0,"ethrejected":0,"ethshares":2,"fanpercentages":[31,36,30,30],"pooladdrs":"us1.ethermine.org:4444","powerusages":[87.186999999999998,43.200000000000003,85.253,83.920000000000002],"runtime":"1","temperatures":[62,54,60,60],"version":"0.15.0.dev8-3+commit.a6bf159e"}}
		public StatusHR(JSONObject object) {
			this.setJSONObject(object);
			if(object.containsKey("version")) {
				this.setVersion((String) object.get("version"));
			}
			if(object.containsKey("runtime")) {
				this.setRuntime(Integer.valueOf((String) object.get("runtime")));
			}
			if(object.containsKey("ethhashrate")) {
				this.setHashrate((long) object.get("ethhashrate") / 1000000);
			}
			if(object.containsKey("ethshares")) {
				this.setShares((long) object.get("ethshares"));
			}
			if(object.containsKey("ethinvalid")) {
				this.setInvalid((long) object.get("ethinvalid"));
			}
			if(object.containsKey("ethhashrates")) {
				JSONArray array = (JSONArray) object.get("ethhashrates");
				gpu_hashrate = new double[array.size()];
				for(int i=0;i<array.size();i++) {
					gpu_hashrate[i] = ((long) array.get(i)) / 1000000d;
				}
			}
			if(object.containsKey("fanpercentages")) {
				JSONArray array = (JSONArray) object.get("fanpercentages");
				gpu_fan_precentage = new int[array.size()];
				for(int i=0;i<array.size();i++) {
					int v = Integer.valueOf(String.valueOf((long) array.get(i))); //Boy do I hate doing this but the JSON Library makes me
					gpu_fan_precentage[i] = v;
					AVG_FAN += v;
				}
				AVG_FAN = AVG_FAN / (double) array.size();
			}
			if(object.containsKey("powerusages")) {
				JSONArray array = (JSONArray) object.get("powerusages");
				gpu_power = new double[array.size()];
				for(int i=0;i<array.size();i++) {
					double v = (double) array.get(i);
					gpu_power[i] = v;
					TOTAL_POWER += v;
				}
			}
			if(object.containsKey("temperatures")) {
				JSONArray array = (JSONArray) object.get("temperatures");
				gpu_temperature = new int[array.size()];
				for(int i=0;i<array.size();i++) {
					int v = Integer.valueOf(String.valueOf((long) array.get(i)));
					gpu_temperature[i] = v;
					AVG_TEMP += v;
				}
				AVG_TEMP = AVG_TEMP / (double) array.size();
			}
			
			if(object.containsKey("pooladdrs")) {
				this.setPool((String) object.get("pooladdrs"));
			}
			
		}
		/*
		response["version"] = version.str();		// miner version.
		response["runtime"] = runtime.str();		// running time, in minutes.
		// total ETH hashrate in MH/s, number of ETH shares, number of ETH rejected shares.
		response["ethhashrate"] = (p.rate());
		response["ethhashrates"] = detailedMhEth;  
		response["ethshares"] 	= s.getAccepts(); 
		response["ethrejected"] = s.getRejects();   
		response["ethinvalid"] 	= s.getFailures(); 
		response["ethpoolsw"] 	= 0;             
		// Hardware Info
		response["temperatures"] = temps;             		// Temperatures(C) for all GPUs
		response["fanpercentages"] = fans;             		// Fans speed(%) for all GPUs
		response["powerusages"] = powers;         			// Power Usages(W) for all GPUs
		response["pooladdrs"] = poolAddresses.str();        // current mining pool. For dual mode, there will be two pools here.
	 */

		public JSONObject getJSONObject() {
			return object;
		}

		public void setJSONObject(JSONObject object) {
			this.object = object;
		}

		public String getVersion() {
			return SOFTWARE_VERSION;
		}

		public void setVersion(String sOFTWARE_VERSION) {
			SOFTWARE_VERSION = sOFTWARE_VERSION;
		}

		public int getRuntime() {
			return RUNTIME;
		}

		public void setRuntime(int rUNTIME) {
			RUNTIME = rUNTIME;
		}

		public float getHashrate() {
			return HASHRATE;
		}

		public void setHashrate(float hASHRATE) {
			HASHRATE = hASHRATE;
		}

		public long getShares() {
			return SHARES;
		}

		public void setShares(long l) {
			SHARES = l;
		}

		public long getInvalid() {
			return INVALID;
		}

		public void setInvalid(long l) {
			INVALID = l;
		}

		public double getAvgTemp() {
			return AVG_TEMP;
		}

		public double getAvgFan() {
			return AVG_FAN;
		}

		public String getPool() {
			return POOL;
		}

		public void setPool(String pOOL) {
			POOL = pOOL;
		}

		public int getSpecificTemp(int gpu) {
			return gpu_temperature[gpu];
		}

		public int getSpecificFan(int gpu) {
			return gpu_fan_precentage[gpu];
		}

		public double getSpecificPower(int gpu) {
			return gpu_power[gpu];
		}

		public double getTotalPower() {
			return TOTAL_POWER;
		}

		public double getSharesPerMin() {
			return ((double) this.getShares()) / (double) this.getRuntime();
		}

		public int getAmtGPUs() {
			return gpu_hashrate.length;
		}

		public double getGPURate(int gpu) {
			return gpu_hashrate[gpu];
		}

		public String toString() {
			return object.toString();
		}
		
	}