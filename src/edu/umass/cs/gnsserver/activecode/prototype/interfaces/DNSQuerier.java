package edu.umass.cs.gnsserver.activecode.prototype.interfaces;

import java.util.List;

import edu.umass.cs.gnsserver.activecode.prototype.utils.Location;

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
	 * @param ips 
	 * @return 
	 */
	public List<Location> getLocations(List<String> ips);
		
}
