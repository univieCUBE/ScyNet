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

import javax.swing.*;
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


	public ContextualizeWithFluxDataTask(CyNetworkView view, CyApplicationManager cyApplicationManager, HashMap<String, Double> tsvMap){
		super(view);
		this.cyApplicationManager = cyApplicationManager;
		this.tsvMap = tsvMap;
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
}
