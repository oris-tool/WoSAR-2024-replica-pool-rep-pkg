package unifi.stlab;

import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

import java.math.BigDecimal;

public interface PoolRejuvenationBuilder {

    void build(PetriNet net, Marking marking);

    void changeTrigger(PetriNet net, String triggerValue);

    void changePoolsize(PetriNet net, Marking marking, int poolSize);

    String getUnavailabilityReward();

    String getUnreliabilityReward();

    String getPerformabilityReward(int bound);

    String getPerformabilityReward();

    String getModulatedUnreliabilityReward(int module);

    String getNumberedUnreliabilityReward(int module);

    String getNickName();
}
