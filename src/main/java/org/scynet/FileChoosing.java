package org.scynet;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

/**
 * A class used to select a file which is used to create a HashMap, which maps all reactions/edges to a Flux.
 */
public class FileChoosing {

    private final Logger logger;
    /**
     * The chosen file (should be TSV-format)
     */
    private File chosenFile;

    public Boolean isFva;

    /**
     * This opens a JFileChooser where a TSV-file can be selected.
     */
    public FileChoosing()
    {
        this.logger = Logger.getLogger(CyUserLog.NAME);
        this.isFva = false; // Set the default value

        // Here we use the JFileChooser to open a window where the user can select a TSV-file with the fluxes
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the tab-delimited TSV-file or press CANCEL");

        int fileValue = chooser.showDialog(null, "Choose");
        // If there was no file selected here, later we will return the empty tsvMap with makeMap()
        if(fileValue == JFileChooser.APPROVE_OPTION)
        {
            this.chosenFile = chooser.getSelectedFile();
        }
    }

    /**
     * Using the previously added TSV-file a HashMap is created, which maps all the reactions/edges to a certain Flux.
     *
     * @return The Hashmap mapping all edges to a Flux. If no file was added an empty HashMap is returned.
     */
    public HashMap<String, Double> makeMap() {
        HashMap<String, Double> tsvMap = new HashMap<>();
        if (chosenFile == null) {
            logger.warn("No file was selected or file was not read.");
            return tsvMap;
        }
        String line = "";
        try {
            // parsing a TSV file into BufferedReader class constructor
            BufferedReader br = new BufferedReader(new FileReader(chosenFile));
            boolean headerFound = false;
            this.isFva = false;
            while ((line = br.readLine()) != null)
            {
                String[] values = line.split("\t", 0);
                if (!Objects.equals(values[0], "reaction_id")  && headerFound && !isFva) {
                    String key = values[0];
                    tsvMap.put(key, Double.parseDouble(values[1]));
                } else if (!Objects.equals(values[0], "reaction_id")  && headerFound && isFva) {
                    String key = values[0];
                    tsvMap.put(key + "_min", Double.parseDouble(values[1]));
                    tsvMap.put(key + "_max", Double.parseDouble(values[2]));
                } else if (!headerFound && Objects.equals(values[0], "reaction_id")) {
                    headerFound = true;
                    if (Objects.equals(values[1], "flux")){
                        isFva = false;
                        logger.info("File contains single flux values.");
                    } else if (values.length > 2 && Objects.equals(values[1], "min_flux") && Objects.equals(values[2], "max_flux")) {
                        isFva = true;
                        logger.info("File contains flux ranges.");
                    }
                    else {
                        logger.error("File could not be parsed due to incompatible formatting.");
                        return tsvMap;  // Wrong formatting
                    }
                }

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return tsvMap;
    }
}
