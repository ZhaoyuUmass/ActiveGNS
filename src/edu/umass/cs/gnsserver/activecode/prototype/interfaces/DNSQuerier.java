package edu.umass.cs.gnsserver.activecode.prototype.interfaces;

import org.json.JSONArray;

import com.maxmind.geoip2.record.Location;

import edu.umass.cs.gnsserver.activecode.prototype.ActiveException;

/**
 * This interface defines the methods that will be used
 * by workers to make DNS-related queries, e.g., GeoIP,
 * EDNS0 and etc.
 * 
 * @author gaozy
 *
 */
public interface DNSQuerier {
	
	/**
	 * Resolve a list of IP addresses to geographic locs
	 * @param ip
	 * @return an {@code com.maxmind.geoip2.record.Location} object
	 * @throws ActiveException 
	 */
	public Location getLocations(String ip) throws ActiveException;
		
}
