package org.scynet;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;

import javax.swing.*;
import java.util.*;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

/**
 * This class is used to fill the new simpler network with nodes from the old network. The nodes from within
 * the exchange-compartment, the nodes for each of the internal compartments and the exchange compartment node are added.
 */
public class CreateNodes {

    private final Logger logger;
    /**
     * The old original network
     */
    final private CyNetwork oldNetwork;
    /**
     * The newly created simpler network
     */
    private final CyNetwork newNetwork;
    /**
     * Translation from old to new nodes
     */
    private HashMap<CyNode, CyNode> oldToNewNodes;
    /**
     * Translation from compartment name to CyNode
     */
    private HashMap<String, CyNode> compNameToCompNode;
    /**
     * Translation from a compartment CyNode to its name
     */
    private HashMap<CyNode, String> compNodeToCompName;
    /**
     * Translation from a compartment name to an organism name
     */
    private HashMap<String, String> compToOrg;
    /**
     * Set of all compartments
     */
    final private Set<String> allCompartments;
    /**
     * Set of all organisms
     */
    final private Set<String> organisms;
    /**
     * List of all external nodes
     */
    private final List<CyNode> extNodes = new ArrayList<>();
    /**
     * List of all exchange nodes
     */
    private List<CyNode> exchgNodes = new ArrayList<>();
    /**
     * List of all exchange reactions (reactions with metabolites in the exchange compartment)
     */
    private List<CyNode> exchgReactions = new ArrayList<>();
    /**
     * String with ID of the shared compartment -> will be called exchg here
     */
    private String exchgCompID = "";
    /**
     * Hashset of nodes to ignore. I.e. nodes that do not belong to an organism nor the shared compartment.
     */
    private HashSet<CyNode> ignoredNodes = new HashSet<>();

    // Constructor

    /**
     * Fills the newNetwork with the exchange-Nodes from the oldNetwork, as well as the corresponding compartment-Nodes
     * @param oldNetwork is the network to be simplified
     * @param newNetwork is the newly created empty network, which will be filled with nodes
     */
    public CreateNodes(CyNetwork oldNetwork, CyNetwork newNetwork) {
        this.logger = Logger.getLogger(CyUserLog.NAME);
        this.oldNetwork = oldNetwork;
        this.newNetwork = newNetwork;
        this.exchgCompID = getExchgCompID();
        this.allCompartments = createComps();
        /**
         * List of all internal compartments
         */
        // Internal compartments are replaced by organisms
        this.organisms = createOrganisms();
        newNetwork.getDefaultNodeTable().createColumn("type", String.class, true);
        newNetwork.getDefaultNodeTable().createColumn("cross-fed", Boolean.class, true);
        createExchgNodes();
        createExchgReactions();
        addExtNodesToNewNetwork(exchgNodes);
        addCompNodesToNewNetwork(organisms);
    }

    // Private Methods

    /**
     * Creates a Set of compartments from a given network, excluding the exchange compartment which is added in the end.
     * @return A Set of compartment names (as Strings) excluding the exchange compartment.
     */
    private Set<String> createComps() {
        // These are all compartments (external + internal) without the exchange compartment, which is added in the end
        // [exchg, ac0, ae0, ...]
        Set<String> comps = new HashSet<>();

        List<CyNode> allNodes = oldNetwork.getNodeList();
        for (CyNode currentNode : allNodes) {
            String node_type = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("sbml type", String.class);
            if (node_type != null && Objects.equals(node_type, "species")) {
                String comp_name = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("sbml compartment", String.class);
                comps.add(comp_name);
            }
        }
        return comps;
    }

    private Set<String> createOrganisms() {
        compToOrg = new HashMap<>();
        ignoredNodes = new HashSet<>();
        // for comp in all comps
        List<CyNode> allNodes = oldNetwork.getNodeList();
        for (CyNode currentNode : allNodes) {
            String node_type = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("sbml type", String.class);
            // Iterate over all metabolites
            if (node_type != null && Objects.equals(node_type, "species")) {
                String compartment = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("sbml compartment", String.class);
                String org = getPutativeOrganismFromNode(currentNode);
                if (Objects.equals(org, "IGNORE")) {
                    ignoredNodes.add(currentNode);
                    if (allCompartments.contains(compartment)) {
                        allCompartments.remove(compartment);
                    }
                    continue;
                }
                if (compToOrg.containsKey(compartment)) {
                    // Check if org is smaller than current org
                    if (org.length() < compToOrg.get(compartment).length()) {
                        compToOrg.put(compartment, org);
                    }
                }
                else {
                    compToOrg.put(compartment, org);
                }
            }
        }
        // store the organisms extra
        return new HashSet<>(compToOrg.values());
    }

    private String getExchgCompID() {
        List<CyNode> allNodes = oldNetwork.getNodeList();
        String compId = "";
        for (CyNode currentNode : allNodes) {
            String node_type = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("sbml type", String.class);
            String cyId = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("cyId", String.class);
            // Iterate over all metabolites
            if (node_type != null && Objects.equals(node_type, "parameter") && Objects.equals(cyId, "shared_compartment_id")) {
                compId = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("shared name", String.class);
                break;
            }
            if (node_type != null && Objects.equals(node_type, "compartment") && Objects.equals(cyId, "medium")) {
                compId = "medium";
            }
        }
        if (Objects.equals(compId, "")) {
            // Display a warning message that no medium compartment could be found
            logger.error("No shared exchange compartment could be identified in the network.");
            JFrame frame = new JFrame();
            JOptionPane pane = new JOptionPane(
                    "No shared exchange compartment could be identified in the network.\n" +
                            "Please make sure that the network contains either\n" +
                            "\t- A compartment named 'medium' which acts as shared exchange compartment\n" +
                            "\t- A parameter 'shared_compartment_id' which is set to the name of\n" +
                            "\t  the shared exchange compartment" +
                            "\n\nPlease consult the ScyNet documentation for further information on \n"+
                            "metabolic model format requirements.",
                    JOptionPane.ERROR_MESSAGE
            );
            pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
            JDialog dialog = pane.createDialog(frame, "Error: Wrong Network Format");

            dialog.setModal(false);
            dialog.setVisible(true);
        }
        return compId;
    }

    /**
     * Creates a list of nodes in the exchange compartment from the given network.
     */
    private void createExchgNodes() {
        // here a list of nodes in the exchg-compartment is made
        List<CyNode> allNodes = oldNetwork.getNodeList();
        List<CyNode> exchangeNode = new ArrayList<>();

        for (CyNode currentNode : allNodes) {
            if (isIgnoredNode(currentNode)) {continue;}
            // from compartment we can check if it is exchange
            long suid = currentNode.getSUID();
            CyRow node_row = oldNetwork.getDefaultNodeTable().getRow(suid);
            String sbml_type = node_row.get("sbml type", String.class);
            if (!Objects.equals(sbml_type, "species")) {continue;}
            String compartment = node_row.get("sbml compartment", String.class);
            if (Objects.equals(compartment, this.exchgCompID)) {exchangeNode.add(currentNode);}
        }
        this.exchgNodes = exchangeNode;
    }

    /**
     * Creates a list of nodes in the exchange compartment from the given network.
     */
    private void createExchgReactions() {
        // here a list of reaction nodes in the exchg-compartment is made
        List<CyNode> allNodes = oldNetwork.getNodeList();
        List<CyNode> exchangeReactions = new ArrayList<>();

        for (CyNode currentNode : allNodes) {
            if (isIgnoredNode(currentNode)) {continue;}
            // from compartment we can check if it is exchange
            long suid = currentNode.getSUID();
            CyRow node_row = oldNetwork.getDefaultNodeTable().getRow(suid);
            String sbml_type = node_row.get("sbml type", String.class);
            if (Objects.equals(sbml_type, "reaction")) {
                List<CyNode> neighbors = getAllNeighbors(currentNode);
                for (CyNode neighbor : neighbors) {
                    String neighborComp = getCompOfMetaboliteNode(neighbor);
                    if (Objects.equals(neighborComp, this.exchgCompID)) {
                        // Is reaction with exchange metabolite
                        exchangeReactions.add(currentNode);
                        break;
                    }
                }
            }
        }
        this.exchgReactions = exchangeReactions;
    }

    /**
     * Returns a list of all neighbors of the given node in the given direction.
     *
     * @param startingNode the node to get the neighbors of
     * @return a list of all neighbors of the given node
     */
    public List<CyNode> getAllNeighbors (CyNode startingNode){

        List<CyNode> oldNeighbors = new ArrayList<>();

        List<CyEdge> edges = oldNetwork.getAdjacentEdgeList(startingNode, CyEdge.Type.INCOMING);
        for (CyEdge edge : edges) {
            oldNeighbors.add(edge.getSource());
        }
        edges = oldNetwork.getAdjacentEdgeList(startingNode, CyEdge.Type.OUTGOING);
        for (CyEdge edge : edges) {
            oldNeighbors.add(edge.getTarget());
        }

        return oldNeighbors;
    }

    /**
     * Adds external nodes to the new network and creates hash-maps mapping old to new nodes.
     * @param exchgNodes a list of external nodes to add to the new network
     */
    private void addExtNodesToNewNetwork(List<CyNode> exchgNodes) {
        // create the HashMaps to store the node translations
        HashMap<CyNode, CyNode> oldNewTranslation = new HashMap<>();
        HashMap<CyNode, List<CyNode>> newOldTranslation = new HashMap<>();
        HashMap<String, CyNode> alreadyPlaced = new HashMap<>();

        // loop through each external node
        for (CyNode oldNode : exchgNodes) {
            String nodeName = getSharedName(oldNode); // get the name of the node
            CyNode newNode;
            if (!alreadyPlaced.containsKey(nodeName)) { // if we haven't already placed a node with this name
                newNode = newNetwork.addNode(); // create a new node in the new network
                alreadyPlaced.put(nodeName, newNode); // store the new node in the alreadyPlaced HashMap
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("name", nodeName); // set the name attribute of the new node
                String nodeSharedName = oldNetwork.getDefaultNodeTable().getRow(oldNode.getSUID()).get("shared name", String.class); // get the "shared name" attribute of the old node
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("shared name", nodeSharedName); // set the "shared name" attribute of the new node
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("type", "exchange metabolite");
            } else {
                newNode = alreadyPlaced.get(nodeName); // if we've already placed a node with this name, get it from the alreadyPlaced HashMap
            }
            oldNewTranslation.put(oldNode, newNode); // add the old-to-new mapping to the oldNewTranslation HashMap
            newOldTranslation.put(newNode, Arrays.asList(oldNode)); // add the new-to-old mapping to the newOldTranslation HashMap
        }
        this.oldToNewNodes = oldNewTranslation; // store the old-to-new mapping in the class variable
    }

    /**
     * Adds all the compartment nodes to the new network
     * @param compList is a List of all the compartment names
     */
    private void addCompNodesToNewNetwork(Set<String> compList) {
        // here the compartment Nodes are added to the new Network and HashMaps mapping old to new Nodes is created simultaneously
        HashMap<String, CyNode> compNameTranslation = new HashMap<>();
        HashMap<CyNode, String> compNodeTranslation = new HashMap<>();
        for (String compartment : compList) {
            CyNode compNode = newNetwork.addNode();
            newNetwork.getDefaultNodeTable().getRow(compNode.getSUID()).set("name", compartment);
            newNetwork.getDefaultNodeTable().getRow(compNode.getSUID()).set("shared name", compartment);
            newNetwork.getDefaultNodeTable().getRow(compNode.getSUID()).set("type", "community member");
            compNameTranslation.put(compartment, compNode);
            compNodeTranslation.put(compNode, compartment);
        }
        this.compNameToCompNode = compNameTranslation;
        this.compNodeToCompName = compNodeTranslation;
    }

    /**
     * Get-fucntion
     * @param node Metabolite node from the old network
     * @return its compartment only if it is a metabolite, else returns 'unknown'
     */
    private String getCompOfMetaboliteNode(CyNode node) {
        // here we return the compartment of a Node only if it is a Metabolite
        String compartment = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml compartment", String.class);
        String node_type = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml type", String.class);
        if (node_type != null && Objects.equals(node_type, "species")) {
            if (compartment != null) {
                if (allCompartments.contains(compartment)) {return compartment;}
            }
        }
        return "unknown";
    }



    /**
     * Returns the identifier for a given node in the format "Mcpd00000".
     *
     * @param node the node for which to get the identifier
     * @return the identifier of the node in the format "Mcpd00000"
     */
    private String getSharedName(CyNode node) {
        String node_type = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml type", String.class);
        if (node_type != null && Objects.equals(node_type, "species")) {
            String shared_name = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("shared name", String.class);
            if (shared_name != null) {return shared_name;}
        }
        return "ERROR";
    }

    // Public Methods [sorted by output]


    /**
     * Get-function
     * @param node any node from the old network
     * @return the name of its organism
     */
    public String getPutativeOrganismFromNode(CyNode node){
        // here the internal compartment corresponding to a Node is returned, regardless where the Node is placed
        String node_type = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml type", String.class);
        if (node_type != null && Objects.equals(node_type, "species")) {
            String sbmlId = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml id", String.class);
            String compartment = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml compartment", String.class);
            if (Objects.equals(compartment, this.exchgCompID)) {return this.exchgCompID;}
            String[] idParts = sbmlId.split("_");
            String[] compParts = compartment.split("_");
            if (!Objects.equals(idParts[1], compParts[0])) {return "IGNORE";}
            else {
                String organism = idParts[1];
                for (int i = 2; i < idParts.length; i++) {
                    // compartment names must not be the same as organism names and must not by empty -> i-2
                    if ((i-2) < compParts.length && Objects.equals(idParts[i], compParts[i-1])) {
                        organism = organism + "_" + idParts[i];
                    }
                    else {break;}
                }

                return organism;
            }
        }
        return "ERROR";
    }

    /**
     * Get-function translation
     * @param node any node in the old network
     * @return the name of the organism the node belongs to (or exchg)
     */
    public String getOrganismFromNode(CyNode node){
        // here the internal compartment corresponding to a Node is returned, regardless where the Node is placed
        String node_type = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml type", String.class);
        if (node_type != null && Objects.equals(node_type, "species")) {
            String compartment = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml compartment", String.class);
            return compToOrg.get(compartment);
        }
        return "ERROR";
    }

    /**
     * Get-function translation
     * @param node any node in the old network
     * @return the name of the sbml type of the node
     */
    public String getSbmlTypeFromNode(CyNode node){
        // here the internal compartment corresponding to a Node is returned, regardless where the Node is placed
        return oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml type", String.class);
    }

    /**
     * Get-function translation
     * @param oldNode any node in the old network
     * @return the corresponding node in the new network
     */
    public CyNode getNewNode(CyNode oldNode) {
        return oldToNewNodes.get(oldNode);
    }

    /**
     * Get-function
     * @param compName the name of a compartment node in the new network
     * @return its CyNode class object
     */
    public CyNode getCompNodeFromName(String compName) {
        return compNameToCompNode.get(compName);
    }

    /**
     * Get-function
     * @return all external nodes
     */
    public List<CyNode> getExtNodes() {
        return extNodes;
    }

    /**
     * Get-function
     * @return all exchange nodes
     */
    public List<CyNode> getExchgNodes() {
        return exchgNodes;
    }


    /**
     * Get-function
     * @param oldNode any node from the old network
     * @return the shared name listed in the NodeTable
     */
    public String getNodeSharedName(CyNode oldNode) {
        return oldNetwork.getDefaultNodeTable().getRow(oldNode.getSUID()).get("shared name", String.class);
    }

    /**
     * Get-function
     * @return all internal compartments
     */
    public Set<String> getOrganisms(){
        return organisms;
    }

    /**
     * Get-function
     * @return all internal compartments
     */
    public List<CyNode> getExchgReactions(){
        return exchgReactions;
    }

    /**
     * Get-function
     * @return the ID of the shared compartment (exchg compartment)
     */
    public String getSharedCompId(){
        return exchgCompID;
    }

    /**
     * Get function
     * @return the set of nodes to ignore
     */
    public HashSet<CyNode> getIgnoredNodes(){
        return ignoredNodes;
    }

    /**
     * @return Whether the node is in the set of nodes to ignore
     */
    public Boolean isIgnoredNode(CyNode node){
        return ignoredNodes.contains(node);
    }
}