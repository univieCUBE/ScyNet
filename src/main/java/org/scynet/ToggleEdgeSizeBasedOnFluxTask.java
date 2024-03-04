package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskMonitor;

import java.util.Objects;
import java.util.Set;

import static java.lang.Math.abs;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

public class ToggleEdgeSizeBasedOnFluxTask extends AbstractNetworkViewTask {

	private final Logger logger;
	private CyApplicationManager cyApplicationManager;

	public ToggleEdgeSizeBasedOnFluxTask(CyNetworkView view, CyApplicationManager cyApplicationManager){
		super(view);
		this.logger = Logger.getLogger(CyUserLog.NAME);
		this.cyApplicationManager = cyApplicationManager;
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null){
			logger.warn("No network selected. Nothing to do.");
			return;
		}

		if(view == null){
			logger.warn("No network view available for selected network. Nothing to do.");
			return;
		}

		//Check if type and cross-fed columns exist
		CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
		Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultEdgeTable());

		Double defaultEdgeWidth = 10.0d;
		Double minEdgeWidth = 1.0d;
		Double maxEdgeWidth = 50.0d;

		if (columnNames.contains("sbml id") && columnNames.contains("flux")) {
			// Check for case no flux values assigned
			Boolean allFluxNull = true;
			Boolean allWidthDefault = true;
			Double maxFlux = 0.0d;
			for (CyEdge newEdge : currentNetwork.getEdgeList()) {
				Double edgeFlux = currentNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
				View<CyEdge> edgeView = view.getEdgeView(newEdge);
				if (edgeView == null) {
					continue;
				}
				if (edgeFlux != null) {
					allFluxNull = false;
					maxFlux = Math.max(maxFlux, edgeFlux);
					Double edgeWidth = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_WIDTH);
					if (!Objects.equals(edgeWidth, defaultEdgeWidth)) {
						allWidthDefault = false;
					}
				}

			}

			// No flux values given: nothing to do
			if (allFluxNull) {
				logger.warn("No flux values found. Nothing to do.");
				return;
			}

			if (allWidthDefault) {
				// Task: set edge width relative to flux
				logger.info("Setting edge widths relative to flux.");
				Double scalingFactor = (maxEdgeWidth - minEdgeWidth) / maxFlux;
				for (CyEdge newEdge : currentNetwork.getEdgeList()) {
					Double edgeFlux = currentNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
					View<CyEdge> edgeView = view.getEdgeView(newEdge);
					if (edgeView == null) {
						continue;
					}
					if ((edgeFlux != 0.0d) && (edgeFlux != null)) {
						Double edgeWidth = abs(edgeFlux) * scalingFactor + minEdgeWidth;
						if (edgeWidth > maxEdgeWidth) {
							edgeWidth = maxEdgeWidth;
						}
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
					} else if (edgeFlux == 0.0d) {
						Double edgeWidth = 0.0d;
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
					}
				}
			}
			else {
				// Task: set edge width to default (toggle off)
				logger.info("Setting edge widths to default.");
				for (CyEdge newEdge : currentNetwork.getEdgeList()) {
					View<CyEdge> edgeView = view.getEdgeView(newEdge);
					if (edgeView == null) {
						continue;
					}
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, defaultEdgeWidth);
				}
			}
		}
		else {
			logger.error("The selected network is not in ScyNet format.");
		}

//
	}
}
