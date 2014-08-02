package no.uio.ifi.vizpub.utils;

/**
 * Class for storing log category strings.
 *
 * Usually, such constants should be represented as an Enum, but in this case strings are more
 * convenient as the minlog library does not utilize any printing method.
 * @author Nils Peder Korsveien
 */
public final class LogCategory {
    public static final String REPORTER = "Reporter";
    public static final String ENGINE_NET = "EngineNet";
    public static final String SIMULATOR = "Simulator";
    public static final String COLLECTOR = "Collector";
    public static final String BOOTSTRAP_CLIENT = "BootstrapClient";
    public static final String BOOTSTRAP_SERVER = "BootstrapServer";
    public static final String COORDINATOR = "Coordinator";
    public static final String POLDERCAST = "PolderCast";
    public static final String COMMONSTATE = "CommonState";
    public static final String PUBLISHING = "Publishing";
    public static final String GEXF = "Gexf";
    public static final String CHURN = "Churn";
}
