package org.cytoscape.sample.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;

import java.util.*;

/**
 * This class is used to fill the new simpler network with nodes from the old network. The nodes from within
 * the exchange-compartment, the nodes for each of the internal compartments and the exchange compartment node are added.
 */
public class CreateNodes {

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
     * Translation from new to old nodes
     */
    private HashMap<CyNode, List<CyNode>> newToOldNodes;
    /**
     * Translation from compartment name to CyNode
     */
    private HashMap<String, CyNode> compNameToCompNode;
    /**
     * Translation from a compartment CyNode to its name
     */
    private HashMap<CyNode, String> compNodeToCompName;
    /**
     * Translation from external node names to a List of CyNodes
     */
    private final HashMap<String, List<CyNode>> extNamesToNodes = new HashMap<>();
    /**
     * Set of all compartments
     */
    final private Set<String> allCompartments;
    /**
     * List of all internal compartments
     */
    final private List<String> internalCompartments;
    /**
     * List of all external nodes
     */
    private final List<CyNode> extNodes = new ArrayList<>();
    /**
     * List of all exchange nodes
     */
    private List<CyNode> exchgNodes = new ArrayList<>();

    // Constructor

    /**
     * Fills the newNetwork with the exchange-Nodes from the oldNetwork, as well as the corresponding compartment-Nodes
     * @param oldNetwork is the network to be simplified
     * @param newNetwork is the newly created empty network, which will be filled with nodes
     */
    public CreateNodes(CyNetwork oldNetwork, CyNetwork newNetwork) {
        this.oldNetwork = oldNetwork;
        this.newNetwork = newNetwork;
        this.allCompartments = createComps();
        this.internalCompartments = createIntComps();
        createExtNodes();
        createExchgNodes();
        addExtNodesToNewNetwork(exchgNodes);
        addCompNodesToNewNetwork(internalCompartments);
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
            if (oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("sbml id", String.class) != null) {
                String currentId = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("sbml id", String.class);
                if (currentId.substring(Math.max(currentId.length() - 2, 0)).equals("c0")) {
                    String[] listId = currentId.split("_", 0);
                    if (listId.length > 3) {
                        comps.add(listId[1].concat("c0"));
                        comps.add(listId[1].concat("e0"));

                    }
                }
            }
        }
        comps.add("exchg");

        return comps;
    }

    /**
     * Creates a list of all internal compartments by removing the external ones from a given list of compartments.
     * @return A List of compartment names (as Strings) with the suffix "_e" removed.
     */
    private List<String> createIntComps() {
        // makes a list of all internal compartments by removing the external ones from all compartments
        // [exchg, ac0, ...]
        List<String> intCompNodeNames = new ArrayList<>();
        for (String compartment : allCompartments) {
            if (compartment.charAt(compartment.length() - 2) != 'e') {
                intCompNodeNames.add(compartment);
            }
        }
        // intCompNodeNames.remove("exchg");
        return intCompNodeNames;
    }

    /**
     * Creates a list of external Nodes from a given network, where external nodes are defined as those associated with compartments with a suffix of "e**" (external) or "exchg" (exchange).
     * For every differing 'shared name' a node is defined, and a dictionary connecting the name to all its ancestors is created.
     */
    private void createExtNodes() {
        // here a list of external Nodes is created by looping through all Nodes and only adding one Node of a certain type (shared name)
        // we also create a dictionary connecting the name to all its ancestors
        // for every differing 'shared name' a node is defined
        List<CyNode> allNodes = oldNetwork.getNodeList();
        List<String> externalNodeNames = new ArrayList<>();

        for (CyNode currentNode : allNodes) {
            String currentComp = getCompOfMetaboliteNode(currentNode);
            if (currentComp.charAt(currentComp.length() - 2) == 'e' || currentComp.equals("exchg")) {
                //String currentName = oldNetwork.getDefaultNodeTable().getRow(currentNode.getSUID()).get("shared name", String.class);
                String currentName = getIdentityM(currentNode);
                // This gets an ID which is equal for equal 'shared name'. Which is accessed by sbml id 'M' + 'cpd00000'
                if (!externalNodeNames.contains(currentName)) {
                    extNodes.add(currentNode);
                    externalNodeNames.add(currentName);
                    List<CyNode> externalNodeList = new ArrayList<>();
                    externalNodeList.add(currentNode);
                    extNamesToNodes.put(currentName, externalNodeList);
                } else {
                    extNodes.add(currentNode);
                    extNamesToNodes.get(currentName).add(currentNode);
                }
            }
        }
    }

    /**
     * Creates a list of nodes in the exchange compartment from the given network.
     */
    private void createExchgNodes() {
        // here a list of nodes in the exchg-compartment is made
        List<CyNode> allNodes = oldNetwork.getNodeList();
        List<CyNode> exchangeNode = new ArrayList<>();

        for (CyNode currentNode : allNodes) {
            // from ID we can get 'M' + 'exchg'
            // Change to retrieval of compartment from compartment field, not sbml id
            long suid = currentNode.getSUID();
            CyRow node_row = oldNetwork.getDefaultNodeTable().getRow(suid);
            String compartment = node_row.get("sbml compartment", String.class);
            //if (sbml_id == null) {sbml_id = new String("null");}
            //String[] idParts = sbml_id.split("_");
            if (Objects.equals(compartment, "exchg")) {exchangeNode.add(currentNode);}
        }
        this.exchgNodes = exchangeNode;
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
            String nodeName = getIdentityM(oldNode); // get the name of the node
            CyNode newNode;
            if (!alreadyPlaced.containsKey(nodeName)) { // if we haven't already placed a node with this name
                newNode = newNetwork.addNode(); // create a new node in the new network
                alreadyPlaced.put(nodeName, newNode); // store the new node in the alreadyPlaced HashMap
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("name", nodeName); // set the name attribute of the new node
                String nodeSharedName = oldNetwork.getDefaultNodeTable().getRow(oldNode.getSUID()).get("shared name", String.class); // get the "shared name" attribute of the old node
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("shared name", nodeSharedName); // set the "shared name" attribute of the new node
            } else {
                newNode = alreadyPlaced.get(nodeName); // if we've already placed a node with this name, get it from the alreadyPlaced HashMap
            }
            oldNewTranslation.put(oldNode, newNode); // add the old-to-new mapping to the oldNewTranslation HashMap
            newOldTranslation.put(newNode, Arrays.asList(oldNode)); // add the new-to-old mapping to the newOldTranslation HashMap
        }
        this.oldToNewNodes = oldNewTranslation; // store the old-to-new mapping in the class variable
        this.newToOldNodes = newOldTranslation; // store the new-to-old mapping in the class variable
    }

    /**
     * Adds all the compartment nodes to the new network
     * @param compList is a List of all the compartment names
     */
    private void addCompNodesToNewNetwork(List<String> compList) {
        // here the compartment Nodes are added to the new Network and HashMaps mapping old to new Nodes is created simultaneously
        HashMap<String, CyNode> compNameTranslation = new HashMap<>();
        HashMap<CyNode, String> compNodeTranslation = new HashMap<>();
        for (String compartment : compList) {
            CyNode compNode = newNetwork.addNode();

            newNetwork.getDefaultNodeTable().getRow(compNode.getSUID()).set("shared name", compartment);
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
        if (oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml id", String.class) != null) {
            String currentId = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml id", String.class);
            if (currentId.length() > 0 && currentId.charAt(0) == 'M') {
                String comp = getCompOfSBML(currentId);
                if (allCompartments.contains(comp)) {return comp;}
            }
        }
        return "unknown";
    }

    /**
     * Get-function
     * @param sbmlId the 'sbml id' value of a certain node
     * @return translates the ID into the name of the corresponding compartment (String)
     */
    private String getCompOfSBML(String sbmlId) {
        // this method is used to translate every sbmlId-string into the corresponding compartment
        String ending = sbmlId.substring(Math.max(sbmlId.length() - 2, 0));
        switch (ending) {
            case "c0": {
                String[] listId = sbmlId.split("_", 0);
                if (listId.length > 3) {
                    return listId[1].concat("c0");
                } else {
                    return sbmlId;
                }
            }
            case "e0": {
                String[] listId = sbmlId.split("_", 0);
                if (listId.length > 3) {
                    return listId[1].concat("e0");
                } else {
                    return sbmlId;
                }
            }
            case "hg": {
                return "exchg";
            }
        }
        return "exchg";
    }

    /**
     * Get-function
     * @param node any node from the old network
     * @return its corresponding compartment if it has one, else 'unknown'
     */
    private String getCompOfNode(CyNode node) {
        // here the compartment of a Node is calculated using its sbmlId
        if (oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml id", String.class) != null) {
            String currentId = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml id", String.class);
            return getCompOfSBML(currentId);
        }
        return "unknown";
    }

    /**
     * Returns the identifier for a given node in the format "Mcpd00000".
     *
     * @param node the node for which to get the identifier
     * @return the identifier of the node in the format "Mcpd00000"
     */
    private String getIdentityM(CyNode node) {
        String[] idParts = oldNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("sbml id", String.class).split("_");
        if (idParts[0].equals("M")) {
            if (idParts.length == 3) {
                return idParts[1];
            } else {
                return idParts[2];
            }
        }
        return "ERROR";
    }

    // Public Methods [sorted by output]

    /**
     * Get-function
     * @param node any node from the old network
     * @return its corresponding internal compartment
     */
    public CyNode getIntCompNodeForAnyNode(CyNode node){
        // here the internal compartment corresponding to a Node is returned, regardless where the Node is placed
        String compartment = getCompOfNode(node);

        if (compartment.charAt(compartment.length() - 2) == 'e'){
            String comp = compartment.substring(0,compartment.length() - 2).concat("c0");
            return getCompNodeFromName(comp);
        } else {
            return getCompNodeFromName(compartment);
        }
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
     * Get-function translation
     * @param newNode a node in the new network
     * @return the corresponding node in the old network
     */
    public List<CyNode> getOldNode(CyNode newNode) {
        return newToOldNodes.get(newNode);
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
     * Get-function translation
     * @param nodeName the external nodes name
     * @return the CyNode class object of it
     */
    public List<CyNode> getExtNodesFromName(String nodeName) {
        return extNamesToNodes.get(nodeName);
    }

    /**
     * Get-function translation
     * @param compNode the compartments node in the new network
     * @return its name as a String
     */
    public String getCompNameFromNode(CyNode compNode) {
        return compNodeToCompName.get(compNode);
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
     * @return all compartments
     */
    public Set<String> getAllComps() {
        return allCompartments;
    }

    /**
     * Get-function
     * @return all internal compartments
     */
    public List<String> getIntComps(){
        return internalCompartments;
    }
}