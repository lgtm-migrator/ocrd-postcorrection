package de.lmu.cis.ocrd.cli;

import de.lmu.cis.ocrd.ml.features.OCRToken;

interface Protocol {
    // This function returns the json representation of the protocol as string.
    String toJSON();

    // Register an OCRToken with the information if the token was considered for
    // the extension step.
    void register(OCRToken token, boolean considered);
}
