package org.scynet;


import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.*;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * A task-class which executes all the needed steps to create a new network-view.
 */
public class CreateNetworkViewTask extends AbstractNetworkTask {

	/**
	 * The factory for creating networks
	 */
	private final CyNetworkFactory cnf;
	/**
	 * The factory for creating network views
	 */
	private final CyNetworkViewFactory cnvf;
	/**
	 * The manager for network views in Cytoscape
	 */
	private final CyNetworkViewManager networkViewManager;
	/**
	 * The manager for networks in Cytoscape
	 */
	private final CyNetworkManager networkManager;
	/**
	 * The naming service for networks in Cytoscape
	 */
	private final CyNetworkNaming cyNetworkNaming;
	/**
	 * The current network in Cytoscape
	 */
	private final CyNetwork currentNetwork;
	/**
	 * The layout algorithm manager in Cytoscape
	 */
	private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
	/**
	 * TSV-map created from the TSV-file if it was added
	 */
	private final HashMap<String, Double> tsvMap;
	/**
	 * The boolean defined by the 'crossfeeding' toggle-button
	 */
	private boolean showOnlyCrossfeeding;
	/**
	 * The boolean defining if the submitted flux map is fva or fba
	 */
	private boolean isFva;

	/**
	 * A task-class which executes all the needed steps to create a new network-view.
	 * @param cyNetworkNaming the naming service for networks in Cytoscape
	 * @param cnf the factory for creating networks
	 * @param networkManager the manager for networks in Cytoscape
	 * @param cnvf the factory for creating network views
	 * @param networkViewManager the manager for network views in Cytoscape
	 * @param tsvMap the loaded TSV-file for the Fluxes
	 * @param showOnlyCrossfeeding the boolean of the toggle-button (Show 'crossfeeding')
	 */
	public CreateNetworkViewTask(CyNetwork network, CyNetworkNaming cyNetworkNaming, CyNetworkFactory cnf, CyNetworkManager networkManager,
								 CyNetworkViewFactory cnvf, final CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager cyLayoutAlgorithmManager,
								 HashMap<String, Double> tsvMap, boolean showOnlyCrossfeeding, CyApplicationManager cyApplicationManager, Boolean isFva) {
		super(network);
		this.cnf = cnf;
		this.cnvf = cnvf;
		this.networkViewManager = networkViewManager;
		this.networkManager = networkManager;
		this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
		this.cyNetworkNaming = cyNetworkNaming;
		this.currentNetwork = cyApplicationManager.getCurrentNetwork();
		this.tsvMap = tsvMap;
		this.isFva = isFva;
		this.showOnlyCrossfeeding = showOnlyCrossfeeding;
	}

	@Override
	public void run(TaskMonitor monitor) throws FileNotFoundException {
		if (currentNetwork == null) {
			// Display a warning message that the network is not in the correct format
			JFrame frame = new JFrame();
			JOptionPane pane = new JOptionPane(
					"No network selected.\n" +
							"Please select a network created or imported by Cy3sbml.",
					JOptionPane.ERROR_MESSAGE
			);
			pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
			JDialog dialog = pane.createDialog(frame, "Error: Missing network");

			dialog.setModal(false);
			dialog.setVisible(true);
			return;
		}
		// Check that the current network is a Cy3sbml network!
		Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultNodeTable());

		if (!(columnNames.contains("sbml type") && columnNames.contains("sbml compartment"))) {
			// Display a warning message that the network is not in the correct format
			JFrame frame = new JFrame();
			JOptionPane pane = new JOptionPane(
					"The selected network is not in Cy3sbml format.\n" +
							"Please select a network created or imported by Cy3sbml.",
					JOptionPane.ERROR_MESSAGE
			);
			pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
			JDialog dialog = pane.createDialog(frame, "Error: Wrong Network Format");

			dialog.setModal(false);
			dialog.setVisible(true);
			return;
		}


		// HERE I CREATE THE NEW NETWORK WHICH WE FILL WITH NEW STUFF
		CyNetwork newNetwork = this.cnf.createNetwork();

		// root network
		CyRootNetwork oldRootNetwork = ((CySubNetwork) currentNetwork).getRootNetwork();
		String name;
		if (oldRootNetwork != null) {
			String oldName = oldRootNetwork.getRow(oldRootNetwork).get(CyNetwork.NAME, String.class);
			name = String.format("%s", oldName);
		}
		else {
			name = "Simplified Community Network";
		}

		// root network
		CyRootNetwork rootNetwork = ((CySubNetwork) newNetwork).getRootNetwork();
		if (rootNetwork != null) {
			rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, String.format("%s", "ScyNet: " + name));
		}

		// My Code goes here
		CreateNodes createNodes = new CreateNodes(currentNetwork, newNetwork);
		CreateEdges createEdges = new CreateEdges(currentNetwork, newNetwork, createNodes, tsvMap, isFva);

		// Here I add a name to my Network
		newNetwork.getDefaultNetworkTable().getRow(newNetwork.getSUID()).set("name", cyNetworkNaming.getSuggestedNetworkTitle("ScyNet: " + name));
		this.networkManager.addNetwork(newNetwork);

		final Collection<CyNetworkView> views = networkViewManager.getNetworkViews(newNetwork);
		CyNetworkView myView = null;
		if (views.size() != 0)
			myView = views.iterator().next();

		if (myView == null) {
			// create a new View for my network
			myView = cnvf.createNetworkView(newNetwork);
			networkViewManager.addNetworkView(myView);
		} else {
			System.out.println("This Network View already existed.");
		}
		// Here the color/size/label etc. of the Nodes and Edges is changed
		Aesthetics aesthetics = new Aesthetics(createNodes, newNetwork, myView, showOnlyCrossfeeding, tsvMap, isFva);

		// Apply the scynet layout
		ApplyScynetLayoutTaskFactory scynetLayoutTF = new ApplyScynetLayoutTaskFactory(cyLayoutAlgorithmManager);
		TaskIterator tItr = scynetLayoutTF.createTaskIterator(myView);
		Task nextTask = tItr.next();
		try {
			nextTask.run(monitor);
		} catch (Exception e) {
			throw new RuntimeException("Could not finish layout", e);
		}
	}
}