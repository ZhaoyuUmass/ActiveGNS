package edu.umass.cs.gnsserver.activecode.prototype.utils;

/**
 * @author gaozy
 *
 */
public class Location {
	
	private final float latitude;
	private final float longitude;
	
	/**
	 * @param latitude
	 * @param longitude
	 */
	public Location(float latitude, float longitude){
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	/**
	 * @return latitude
	 */
	public float getLatitude(){
		return latitude;
	}
	
	/**
	 * @return longitude
	 */
	public float getLongitude(){
		return longitude;
	}
}
