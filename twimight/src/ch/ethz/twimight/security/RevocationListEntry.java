/*******************************************************************************
 * Copyright (c) 2011 ETH Zurich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Paolo Carta - Implementation
 *     Theus Hossmann - Implementation
 *     Dominik Schatzmann - Message specification
 ******************************************************************************/

package ch.ethz.twimight.security;

import java.util.Date;

/**
 * Models an entry of the revocation list
 * @author thossmann
 *
 */
public class RevocationListEntry {

	private String serial;
	private Date until;
	
	/**
	 * Constructor
	 * @param serial
	 * @param until
	 */
	public RevocationListEntry(String serial, Date until){
		setSerial(serial);
		setUntil(until);
	}
	
	/**
	 * @param serial the serial to set
	 */
	public void setSerial(String serial) {
		this.serial = serial;
	}
	/**
	 * @return the serial
	 */
	public String getSerial() {
		return serial;
	}
	/**
	 * @param until the until to set
	 */
	public void setUntil(Date until) {
		this.until = until;
	}
	/**
	 * @return the until
	 */
	public Date getUntil() {
		return until;
	}
	
	
}
