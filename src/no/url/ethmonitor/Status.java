package no.url.ethmonitor;


public abstract class Status {
	String SOFTWARE_VERSION;
	int RUNTIME;
	float HASHRATE;
	int SHARES;
	int INVALID;
	double AVG_TEMP = 0;
	double AVG_FAN = 0;
	String POOL;


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

	public int getShares() {
		return SHARES;
	}

	public void setShares(int sHARES) {
		SHARES = sHARES;
	}

	public int getInvalid() {
		return INVALID;
	}

	public void setInvalid(int iNVALID) {
		INVALID = iNVALID;
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

	public abstract int getSpecificTemp(int gpu);

	public abstract int getSpecificFan(int gpu);
	
	//public abstract double getSpecificPower(int gpu);

	//public abstract double getTotalPower();

	public abstract double getSharesPerMin();

	public abstract int getAmtGPUs();

	public abstract double getGPURate(int gpu);

	public abstract String toString();
	
	public int safeLongToInt(long l) {
	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
	        throw new IllegalArgumentException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}

}
