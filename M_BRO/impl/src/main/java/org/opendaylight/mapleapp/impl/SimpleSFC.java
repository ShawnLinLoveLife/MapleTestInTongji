/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mapleapp.impl;

import org.opendaylight.maple.core.increment.app.MapleAppBase;
import org.opendaylight.maple.core.increment.app.MapleUtil;
import org.opendaylight.maple.core.increment.packet.Ethernet;
import org.opendaylight.maple.core.increment.packet.IPv4;
import org.opendaylight.maple.core.increment.tracetree.MaplePacket;
import org.opendaylight.maple.core.increment.tracetree.Port;
import org.opendaylight.maple.core.increment.tracetree.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SimpleSFC extends MapleAppBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SimpleSFC.class);

	private static final String TOPO_URL       = "/root/network-topology/topology";
	private static final String HOST_TABLE_URL = "/root/host-table";
	private static final String FLOW_FILE_NAME_URL  = "/root/flow-file-name";

	private static final String      H1    = "192.168.0.11";
	private static final int H1_IP = IPv4.toIPv4Address(H1);

	private static final String      H3    = "192.168.0.13";
	private static final int H3_IP = IPv4.toIPv4Address(H3);

	private static final String      H_BRO = "192.168.0.15";
	private static final int HBRO_IP = IPv4.toIPv4Address(H_BRO);

	private static final String[] H13_FAST_PATH = { "openflow:966034607844:3", "openflow:966034607668:2", "openflow:949187772416:1" };
	private static final String[] H13_SLOW_PATH  = { "openflow:966034607844:4", "openflow:949187772416:1" };
	private static final String[] H31_FAST_PATH = { "openflow:949187772416:4", "openflow:966034607668:1", "openflow:966034607844:1" };
	private static final String[] H31_SLOW_PATH  = { "openflow:949187772416:3", "openflow:966034607844:1" };

	private String flow2Filename(FlowId flowId) {

		return readData(FLOW_FILE_NAME_URL).get(flowId);

	}

	private void forwardToBro(MaplePacket pkt) {

		// Get information for computing the path between src and Bro
		Map<Integer, Port> hostTable = (Map<Integer, Port>) readData(HOST_TABLE_URL);
		int srcIP = pkt.IPv4Src();
		Port srcPort = hostTable.get(srcIP);
		Port dstPort = hostTable.get(HBRO_IP);
		Topology topo = (Topology) readData(TOPO_URL);

		// Compute the path, and send the packet to the BRO Server
		pkt.addRoute(MapleUtil.shortestPath(topo.getLink(), srcPort, dstPort));

	}

	public void onPacket(MaplePacket pkt) {

		int ethType = pkt.ethType();

		// For IPv4 traffic only
		if ( ethType == Ethernet.TYPE_IPv4 ) {


			if ( pkt.IPv4SrcIs(H1_IP) && pkt.IPv4DstIs(H3_IP) ) {

				// TODO: Need to decide the key of the mapping between flows and filenames.
				String fn = flow2Filename(pkt.flow);

				if ( fn == null ) {

					// TODO: Need to know this packet is the starting point of a transfer.
					if ( pkt includes the HTTP GET request ) {
						forwardToBro(pkt);
					}

					// At the same time, send the packet to the origin destination
					// TODO: This flow may need hard time out, so we can shift this flow to the fast path later.
					pkt.addRoute(H13_SLOW_PATH);

				}

				// Suppose all science big files start with "SC_"
				// A second pakcet-in may be needed, so that the flow on the slow path can be shifted into the fast
				// path.
				else if ( fn.startsWith("SC_") ) {

					pkt.addRoute(H13_FAST_PATH);

				} else {

						pkt.addRoute(H13_SLOW_PATH);

				}

			} else if ( pkt.IPv4SrcIs(H3_IP) && pkt.IPv4DstIs(H1_IP)) {

				// TODO: Need to know what path this packet belongs to, fast path or slow path
				if ( pkt belongs to FAST_PATH ) {
					pkt.addRoute(H31_FAST_PATH);
				}
				else {
					pkt.addRoute(H31_SLOW_PATH);
				}

			} else {

				pkt.addRoute(Route.Drop);

			}
		} // end of IPv4 Packets

		else {

            // Other type of traffic handled by another Maple App
			passToNext(pkt);

		}
	} // end of onPacket
}



