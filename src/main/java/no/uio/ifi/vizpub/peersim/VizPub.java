package no.uio.ifi.vizpub.peersim;

import no.uio.ifi.vizpub.collector.Collector;
import no.uio.ifi.vizpub.reporter.ReporterService;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;


/**
 * @author Nils Peder Korsveien
 */
public class VizPub implements Control {

    private static final String PAR_CONFIG = "config";
    private final String config;
    private final ReporterService reporterService;
    private final Collector collector;

    public VizPub(String prefix) {
        this.config = Configuration.getString(prefix + "." + PAR_CONFIG);
        collector = new Collector(config);
        reporterService = new ReporterService(config);
        new Thread(collector).start();
        reporterService.startAsync();
    }

    @Override
    public boolean execute() {
        if (CommonState.getPhase() == CommonState.POST_SIMULATION) {
            reporterService.triggerShutdown();
//            reporterService.awaitTerminated();
        }
        return false;
    }

}

