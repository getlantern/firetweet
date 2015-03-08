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

package ch.ethz.twimight.net.tds;

public class TDSPublicKey {
	private long twitterID;
	private String pemKey;
	
	public TDSPublicKey(long twitterId, String pemKey){
		setTwitterID(twitterId);
		setPemKey(pemKey);
	}

	/**
	 * @param twitterID the twitterID to set
	 */
	public void setTwitterID(long twitterID) {
		this.twitterID = twitterID;
	}

	/**
	 * @return the twitterID
	 */
	public long getTwitterID() {
		return twitterID;
	}

	/**
	 * @param pemKey the pemKey to set
	 */
	public void setPemKey(String pemKey) {
		this.pemKey = pemKey;
	}

	/**
	 * @return the pemKey
	 */
	public String getPemKey() {
		return pemKey;
	}
}
