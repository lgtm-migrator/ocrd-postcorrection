package de.lmu.cis.ocrd.cli;

import de.lmu.cis.ocrd.config.Parameters;
import de.lmu.cis.ocrd.pagexml.METSFileGroupProfiler;
import de.lmu.cis.ocrd.pagexml.METSFileGroupReader;
import de.lmu.cis.ocrd.pagexml.Workspace;
import de.lmu.cis.ocrd.profile.AdditionalLexicon;
import de.lmu.cis.ocrd.profile.Profile;

import java.nio.file.Paths;

abstract class ParametersCommand implements Command {
    private final String name;
    private METSFileGroupReader fgr;
    protected Parameters parameters;
    protected Workspace workspace;

    ParametersCommand(String name) {
        this.name = name;
    }

    protected void init(CommandLineArguments config) throws Exception {
        this.parameters = config.mustGetParameter(Parameters.class);
        this.workspace = new Workspace(Paths.get(config.mustGetMETSFile()));
        this.fgr = new METSFileGroupReader(workspace.getMETS(), parameters);
    }

    METSFileGroupReader getFGR() {
        return fgr;
    }

    Profile getProfile(String ifg, AdditionalLexicon alex, int n) throws Exception {
        return new METSFileGroupProfiler(parameters, fgr.getWordReader(ifg), ifg, alex, parameters.getNOCR()).profile();
    }

    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public String getName() {
        return name;
    }
}
