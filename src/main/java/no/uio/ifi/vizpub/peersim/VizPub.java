package no.uio.ifi.vizpub.peersim;

import no.uio.ifi.vizpub.collector.Collector;
import no.uio.ifi.vizpub.reporter.ReporterService;
import peersim.core.Control;

/**
 * @author Nils Peder Korsveien
 */
public class VizPub implements Control {

    private final String config;

    public VizPub(String config) {
       this.config = config;
    }

    @Override
    public boolean execute() {
        Collector collector = new Collector(config);
        new Thread(collector).start();
        ReporterService reporterService = new ReporterService(config);
        reporterService.startAsync();
        reporterService.triggerShutdown();
        reporterService.awaitTerminated();
        return false;
    }
}
