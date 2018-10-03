package no.url.ethmonitor;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class StatusHR extends Status{
	private JSONObject object;
	private double TOTAL_POWER = 0;
	
		public StatusHR(JSONObject object) {
			this.setJSONObject(object);
			if(object.containsKey("version")) {
				this.setVersion((String) object.get("version"));
			}
			if(object.containsKey("runtime")) {
				this.setRuntime(Integer.valueOf((String) object.get("runtime")));
			}
			if(object.containsKey("ethhashrate")) {
				this.setHashrate((long) object.get("ethhashrate") / 1000000f);
			}
			if(object.containsKey("ethshares")) {
				this.setShares((this.safeLongToInt((long) object.get("ethshares"))));
			}
			if(object.containsKey("ethinvalid")) {
				this.setInvalid((this.safeLongToInt((long) object.get("ethinvalid"))));
			}
			if(object.containsKey("fanpercentages")) {
				JSONArray array = (JSONArray) object.get("fanpercentages"); //null?
				if(array != null) {
					for(int i=0;i<array.size();i++) {
						int v = Integer.valueOf(String.valueOf((long) array.get(i))); //Boy do I hate doing this but the JSON Library makes me
						AVG_FAN += v;
					}
					AVG_FAN = AVG_FAN / (double) array.size();					
				}
			}
			if(object.containsKey("powerusages")) {
				JSONArray array = (JSONArray) object.get("powerusages");
				if(array != null) {
					for(int i=0;i<array.size();i++) {
						double v = (double) array.get(i);
						TOTAL_POWER += v;
					}					
				}
			}
			if(object.containsKey("temperatures")) {
				JSONArray array = (JSONArray) object.get("temperatures");
				if(array != null) {
					for(int i=0;i<array.size();i++) {
						int v = Integer.valueOf(String.valueOf((long) array.get(i)));
						AVG_TEMP += v;
					}
					AVG_TEMP = AVG_TEMP / (double) array.size();					
				}
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

		public int getSpecificTemp(int gpu) {
			JSONArray array = (JSONArray) object.get("temperatures");
			if(array != null)
				return Integer.valueOf(String.valueOf((long) array.get(gpu)));
			return 0;
		}

		public int getSpecificFan(int gpu) {
			JSONArray array = (JSONArray) object.get("fanpercentages");
			if(array != null)
				return Integer.valueOf(String.valueOf((long) array.get(gpu)));
			return 0;
		}

		public double getSpecificPower(int gpu) {
			JSONArray array = (JSONArray) object.get("powerusages");
			if(array != null)
				return (double) array.get(gpu);
			return 0D;
		}

		public double getTotalPower() {
			return TOTAL_POWER;
		}

		public double getSharesPerMin() {
			return ((double) this.getShares()) / (double) this.getRuntime();
		}

		public int getAmtGPUs() {
			JSONArray array = (JSONArray) object.get("fanpercentages");
			if(array != null)
				return array.size();
			return 0;
		}

		public double getGPURate(int gpu) {
			JSONArray array = (JSONArray) object.get("ethhashrates");
			if(array != null)
				return ((double) array.get(gpu)) / 1000000d;
			return 0D;
		}

		public String toString() {
			return object.toString();
		}
		
	}