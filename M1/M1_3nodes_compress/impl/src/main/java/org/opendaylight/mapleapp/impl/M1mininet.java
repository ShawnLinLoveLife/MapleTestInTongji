/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mapleapp.impl;

import org.opendaylight.maple.core.increment.app.MapleAppBase;
import org.opendaylight.maple.core.increment.packet.Ethernet;
import org.opendaylight.maple.core.increment.packet.IPv4;
import org.opendaylight.maple.core.increment.tracetree.MaplePacket;
import org.opendaylight.maple.core.increment.tracetree.Route;
import org.slf4j.LoggerFactory;

public class M1mininet extends MapleAppBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(M1mininet.class);


	private static final String      H1    = "192.168.0.11";
	private static final int H1_IP = IPv4.toIPv4Address(H1);

	private static final String      H3    = "192.168.0.13";
	private static final int H3_IP = IPv4.toIPv4Address(H3);

	private static final int HTTP_PORT = 80;

	private static final String[] H13_HIGH_PATH = { "openflow:1:3", "openflow:2:2", "openflow:3:1" };
	private static final String[] H13_LOW_PATH  = { "openflow:1:4", "openflow:3:1" };
	private static final String[] H31_HIGH_PATH = { "openflow:3:4", "openflow:2:1", "openflow:1:1" };
	private static final String[] H31_LOW_PATH  = { "openflow:3:3", "openflow:1:1" };

	@Override
	public void onPacket(MaplePacket pkt) {

		int ethType = pkt.ethType();

		// For IPv4 traffic only
		if ( ethType == Ethernet.TYPE_IPv4) {

			// H1 -> H2
			if ( pkt.IPv4SrcIs(H1_IP) && pkt.IPv4DstIs(H3_IP) ) {

				String[] path = null;

				if ( ! pkt.TCPDstPortIs(HTTP_PORT) ) {  // All non HTTP IP, e.g., UDP, PING, SSH
					path = H13_LOW_PATH;
				} else {                                // Only HTTP traffic
					path = H13_HIGH_PATH;
				}

				pkt.setRoute(path);

				// Other host pairs
			}

			// H2 -> H1
			else if (  pkt.IPv4SrcIs(H3_IP) && pkt.IPv4DstIs(H1_IP) ) {

				String[] path = null;

				if ( ! pkt.TCPSrcPortIs(HTTP_PORT) ) {
					path = H31_LOW_PATH;
				} else {
					path = H31_HIGH_PATH;
				}
				pkt.setRoute(path);

				// All other pairs non than H1 <-> H2
			} else {

				pkt.setRoute(Route.DROP);

			}
		}                       // end of ethType == Ethernet.TYPE_IPv4

		else {                  // Other type of traffic handled by another Maple App
			passToNext(pkt);
		}

	} // end of onPacket
}

