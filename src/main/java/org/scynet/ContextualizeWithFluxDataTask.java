package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import static java.lang.Math.abs;

public class ContextualizeWithFluxDataTask extends AbstractNetworkViewTask {

	private CyApplicationManager cyApplicationManager;
	/**
	 * TSV-map created from the TSV-file if it was added
	 */
	private final HashMap<String, Double> tsvMap;
	/**
	 * The boolean defining if the submitted flux map is fva or fba
	 */
	private boolean isFva;
	/**
	 * The manager for networks in Cytoscape
	 */
	private final CyNetworkManager networkManager;
	/**
	 * The naming service for networks in Cytoscape
	 */
	private final CyNetworkNaming cyNetworkNaming;


	public ContextualizeWithFluxDataTask(CyNetworkView view, CyApplicationManager cyApplicationManager, HashMap<String, Double> tsvMap, Boolean isFva, CyNetworkManager networkManager, CyNetworkNaming cyNetworkNaming){
		super(view);
		this.cyApplicationManager = cyApplicationManager;
		this.tsvMap = tsvMap;
		this.isFva = isFva;
		this.networkManager = networkManager;
		this.cyNetworkNaming = cyNetworkNaming;
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null){			
			return;
		}

		if(view == null){
			return;
		}

		//Check if type and cross-fed columns exist
		CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
		Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultEdgeTable());

		if (columnNames.contains("sbml id") && columnNames.contains("flux")) {
			if (tsvMap.isEmpty()) {
				return;
			}

			// Add flux to edge
			for (CyEdge edge : currentNetwork.getEdgeList()) {
				String fluxKey = currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).get("name", String.class);
				Double fluxValue;

				if (isFva) {
					Double minFluxValue = getFlux(fluxKey, true);
					Double maxFluxValue = getFlux(fluxKey, false);
					currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).set("min flux", minFluxValue);
					currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).set("max flux", maxFluxValue);
					fluxValue = Math.max(Math.abs(minFluxValue), Math.abs(maxFluxValue));
				}
				else {
					fluxValue = getFlux(fluxKey, false);
				}
				currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).set("flux", fluxValue);
			}

			// Add styling to edge

		} else {
			// Display a warning message that the network is not in the correct format
			JFrame frame = new JFrame();
			JOptionPane pane = new JOptionPane(
					"The selected network is not in ScyNet format. " +
							"Please select a network created by ScyNet.",
					JOptionPane.WARNING_MESSAGE
			);
			pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
			JDialog dialog = pane.createDialog(frame, "Error: Wrong Network Format");

			dialog.setModal(false);
			dialog.setVisible(true);
		}

//
	}

	private Double getFlux(String key, Boolean isReverse) {
		if (!isFva && tsvMap.get(key) == null) {
			return 0.0d;
		}
		if (Objects.equals(key, "")) {
			return null;
		} else if (isFva) {
			if (isReverse && tsvMap.get(key + "_min") != null) {
				return tsvMap.get(key + "_min");
			}
			else if (!isReverse && tsvMap.get(key + "_max") != null) {
				return tsvMap.get(key + "_max");
			}
			else {
				return 0.0d;
			}
		} else {
			return tsvMap.get(key);
		}
	}
}
